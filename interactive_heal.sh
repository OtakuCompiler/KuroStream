#!/bin/bash

MAX_ATTEMPTS=40

# Ensure Git identity is set up cleanly
git config --global user.name "OtakuCompiler"
git config --global user.email "otakucompiler@users.noreply.github.com"

# Sync local files with GitHub right away
sync_repo() {
    echo "📥 Checking connection to https://github.com/OtakuCompiler/Kuro-Stream.git..."
    if [ ! -d ".git" ]; then
        git init
        git remote add origin https://github.com/OtakuCompiler/Kuro-Stream.git
        git branch -M main
    fi
    git pull origin main --allow-unrelated-histories --no-edit
}

run_auto_heal() {
    echo "🔍 Locating '.github/workflows' paths..."
    WORKFLOW_DIRS=$(find . -path "*/.github/workflows" -type d)

    if [ -z "$WORKFLOW_DIRS" ]; then
        echo "❌ Error: Could not find a '.github/workflows' directory anywhere."
        return
    fi

    for W_DIR in $WORKFLOW_DIRS; do
        for WORKFLOW_FILE in "$W_DIR"/*.yml "$W_DIR"/*.yaml; do
            [ -e "$WORKFLOW_FILE" ] || continue
            FILENAME=$(basename "$WORKFLOW_FILE")
            
            ATTEMPT=1
            while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
                echo "=========================================================="
                echo "🚀 [$FILENAME] -> ATTEMPT $ATTEMPT / $MAX_ATTEMPTS"
                echo "=========================================================="
                
                echo "📤 Pushing latest modifications upstream..."
                git add "$WORKFLOW_FILE"
                git commit -m "Auto-Heal Check Step $ATTEMPT" --allow-empty
                git push origin main
                
                echo "⏳ Waiting for GitHub runner deployment..."
                sleep 10
                
                RUN_ID=$(gh run list --workflow="$FILENAME" --limit 1 --json databaseId --jq '.[0].databaseId' 2>/dev/null)
                while [ -z "$RUN_ID" ] || [ "$RUN_ID" = "null" ]; do
                    sleep 4
                    RUN_ID=$(gh run list --workflow="$FILENAME" --limit 1 --json databaseId --jq '.[0].databaseId' 2>/dev/null)
                done
                
                echo "🛰️ Connected to Live Run ID: $RUN_ID. Monitoring..."
                while true; do
                    STATUS=$(gh run view "$RUN_ID" --json status,conclusion --jq '.status + " " + .conclusion' 2>/dev/null)
                    if [[ "$STATUS" == "completed success" ]]; then
                        echo "✅ SUCCESS! $FILENAME passed perfectly!"
                        break 2
                    elif [[ "$STATUS" == *completed* ]]; then
                        echo "❌ RUNNER FAILED! Downloading error stack traces..."
                        break
                    fi
                    echo -n "."
                    sleep 4
                done
                
                # Sift logs to keep things super lightweight for OpenCode backend stability
                gh run view "$RUN_ID" --log > raw.txt 2>&1
                grep -C 10 -iE "error|failed|invalid|exception" raw.txt | head -n 80 > simple_error.txt
                if [ ! -s simple_error.txt ]; then tail -n 40 raw.txt > simple_error.txt; fi
                
                echo "🧠 Activating OpenCode Core Engine..."
                opencode run "The GitHub Actions workflow '$WORKFLOW_FILE' failed with this trace:
$(cat simple_error.txt)

Fix the bugs inside '$WORKFLOW_FILE' using your tools and rewrite it cleanly."
                
                rm -f raw.txt simple_error.txt
                ATTEMPT=$((ATTEMPT+1))
                sleep 2
            done
        done
    done
}

# --- INTERACTIVE TERMINAL MENU UI ---
while true; do
    echo "================================================="
    echo " 🛸  OTAKUCOMPILER INTERACTIVE AGENT MENU  🛸"
    echo "================================================="
    echo " 1) 🔄 Sync Local Folder with GitHub Remote"
    echo " 2) ⚡ Start 40-Attempt Auto-Heal Repair Loop"
    echo " 3) 🚪 Exit Terminal Interface"
    echo "================================================="
    read -rp "Select an option [1-3]: " OPTION

    case $OPTION in
        1)
            sync_repo
            ;;
        2)
            sync_repo
            run_auto_heal
            ;;
        3)
            echo "👋 Shutting down Interactive Agent Interface. Execution closed."
            exit 0
            ;;
        *)
            echo "❌ Invalid choice. Select 1, 2, or 3."
            ;;
    esac
    echo ""
done
