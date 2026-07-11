#!/bin/bash

MODEL="nvidia/nemotron-3-ultra"
MAX_ATTEMPTS=40

# Auto-install the local validator if it's missing from the container
if ! command -v yamllint &> /dev/null; then
    echo "📦 Package 'yamllint' missing. Installing local validator..."
    apt-get update && apt-get install -y yamllint
fi

echo "🔍 Scanning directories to locate your '.github/workflows' paths..."
WORKFLOW_DIRS=$(find . -path "*/.github/workflows" -type d)

if [ -z "$WORKFLOW_DIRS" ]; then
    echo "❌ Error: Could not find a '.github/workflows' directory in this folder tree."
    exit 1
fi

for W_DIR in $WORKFLOW_DIRS; do
    echo "📁 Locked onto folder path: $W_DIR"
    
    for WORKFLOW_FILE in "$W_DIR"/*.yml "$W_DIR"/*.yaml; do
        [ -e "$WORKFLOW_FILE" ] || continue
        FILENAME=$(basename "$WORKFLOW_FILE")
        
        echo "=========================================================="
        echo "⚡ LOCAL HIGH-SPEED AUTO-HEAL: $FILENAME"
        echo "=========================================================="
        
        ATTEMPT=1
        while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
            echo "🔄 [Attempt $ATTEMPT/$MAX_ATTEMPTS] Testing syntax structures..."
            
            # Check yaml syntax layouts locally
            yamllint -d "{extends: relaxed, rules: {line-length: disable}}" "$WORKFLOW_FILE" > local_errors.txt 2>&1
            LINT_STATUS=$?
            
            if [ $LINT_STATUS -eq 0 ]; then
                echo "✅ SUCCESS! $FILENAME has clean syntax layout structures."
                rm -f local_errors.txt
                break 
            fi
            
            echo "❌ Structural errors detected! Invoking OpenCode..."
            
            opencode run --model "$MODEL" "CONTEXT ENVIRONMENT:
You are optimizing a local GitHub Actions file layout: '$WORKFLOW_FILE'.
The file failed structural syntax tests. Here are the local linter errors:

--- LOCAL LINTER ERROR TRACE ---
$(cat local_errors.txt)
--- END TRACE ---

INSTRUCTIONS:
1. Review '$WORKFLOW_FILE' using your file reading tools.
2. Fix any spacing layout, wrong keywords, or indentations causing the linter trace errors.
3. Write the fully updated formatting data back into '$WORKFLOW_FILE'."

            echo "🔧 Edits applied locally. Re-running verification track..."
            rm -f local_errors.txt
            ATTEMPT=$((ATTEMPT+1))
            sleep 1
        done
        
        if [ $ATTEMPT -gt $MAX_ATTEMPTS ]; then
            echo "🛑 Max execution attempts hit for $FILENAME."
        fi
    done
done

echo "🎉 All local workflow validation tasks completed!"
exit 0
