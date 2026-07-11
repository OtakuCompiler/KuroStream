#!/bin/bash

MODEL="nvidia/nemotron-3-ultra"
MAX_ATTEMPTS=40

# 🛠️ AUTOMATIC GIT INITIALIZATION FOR NORMAL FOLDERS
if [ ! -d ".git" ]; then
    echo "🗂️ Normal folder detected. Transforming this folder into a Git repository..."
    git init
    
    echo "🔗 Please enter your GitHub Repository URL (e.g., https://github.com/username/KuroStream.git):"
    read -r REPO_URL
    
    if [ -z "$REPO_URL" ]; then
        echo "❌ Error: A GitHub URL is required to push changes. Exiting."
        exit 1
    fi
    
    git remote add origin "$REPO_URL"
    git branch -M main
    
    echo "📦 Creating initial baseline commit..."
    git add .
    git commit -m "Initial commit from auto-heal setup"
    git push -u origin main
fi

echo "🔍 Deep-scanning directories to locate your '.github/workflows' path..."
WORKFLOW_DIRS=$(find . -path "*/.github/workflows" -type d)

if [ -z "$WORKFLOW_DIRS" ]; then
    echo "❌ Error: Could not find a '.github/workflows' directory anywhere in this project tree."
    exit 1
fi

for W_DIR in $WORKFLOW_DIRS; do
    echo "📁 Successfully locked onto workflow directory at: $W_DIR"
    
    for WORKFLOW_FILE in "$W_DIR"/*.yml "$W_DIR"/*.yaml; do
        [ -e "$WORKFLOW_FILE" ] || continue
        FILENAME=$(basename "$WORKFLOW_FILE")
        
        echo "=========================================================="
        echo "⚡ HIGH-SPEED AUTO-HEAL TARGET: $FILENAME"
        echo "📍 TARGET PATH: $WORKFLOW_FILE"
        echo "=========================================================="
        
        ATTEMPT=1
        while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
            echo "📤 [Attempt $ATTEMPT/$MAX_ATTEMPTS] Syncing configurations to GitHub..."
            git add "$WORKFLOW_FILE"
            git commit -m "Pipeline Optimization Pass $ATTEMPT ($FILENAME)" --allow-empty
            git push origin main
            
            echo "⏳ Polling GitHub API for active runner assignment..."
            sleep 8
            
            RUN_ID=$(gh run list --workflow="$FILENAME" --limit 1 --json databaseId --jq '.[0].databaseId' 2>/dev/null)
            
            while [ -z "$RUN_ID" ] || [ "$RUN_ID" = "null" ]; do
                sleep 4
                RUN_ID=$(gh run list --workflow="$FILENAME" --limit 1 --json databaseId --jq '.[0].databaseId' 2>/dev/null)
            done
            
            echo "🛰️ Hooked Run ID: $RUN_ID. Watching live metrics..."
            
            while true; do
                STATUS=$(gh run view "$RUN_ID" --json status,conclusion --jq '.status + " " + .conclusion' 2>/dev/null)
                
                if [[ "$STATUS" == "completed success" ]]; then
                    echo "✅ BUILD PASSED! $FILENAME is working perfectly."
                    break 2 
                elif [[ "$STATUS" == *completed* ]]; then
                    echo "❌ RUNNER FAILED! Isolating error stack traces..."
                    break 
                fi
                echo -n "."
                sleep 4 
            done
            
            gh run view "$RUN_ID" --log > full_raw_logs.txt 2>&1
            grep -C 15 -iE "error|failed|invalid|exception|parse error" full_raw_logs.txt | head -n 120 > isolated_errors.txt
            
            if [ ! -s isolated_errors.txt ]; then
                tail -n 50 full_raw_logs.txt > isolated_errors.txt
            fi
            
            echo -e "\n🧠 Dispatching Streamlined Error Context to OpenCode..."
            
            opencode run --model "$MODEL" "CONTEXT ENVIRONMENT:
You are optimizing the GitHub Actions script: '$WORKFLOW_FILE'.
The runner failed. Here is the highly targeted error trace extracted from the environment logs:

--- ISOLATED FAILURE TRACE ---
$(cat isolated_errors.txt)
--- END TRACE ---

INSTRUCTIONS:
1. Review '$WORKFLOW_FILE' using your file reading tools.
2. Fix only the syntax, bad actions, or blocks causing the error highlighted above.
3. Write the fully updated file back into '$WORKFLOW_FILE'. Do not leave placeholders."

            echo "🔧 Edits applied by OpenCode. Cleaning workspace files..."
            rm -f full_raw_logs.txt isolated_errors.txt
            ATTEMPT=$((ATTEMPT+1))
            echo "🔄 Re-submitting fix to live testing track..."
            sleep 2
        done
        
        if [ $ATTEMPT -gt $MAX_ATTEMPTS ]; then
            echo "🛑 Upper execution cap hit for $FILENAME."
        fi
    done
done

echo "🎉 Optimization loop complete!"
exit 0
