#!/usr/bin/env bash
# =============================================================================
# CHATBOT MANUAL TEST SCRIPT
# Run after the Spring Boot app is started on localhost:8080
# =============================================================================

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
API="$BASE_URL/api/chatbot"

echo "======================================"
echo "Chatbot Manual Test Script"
echo "API Endpoint: $API"
echo "======================================"

# ---------------------------------------------------------------------------
# Helper: send_chat <label> <message>
# ---------------------------------------------------------------------------
send_chat() {
    local label="$1"
    local message="$2"
    echo ""
    echo "▶ [$label] message='$message'"
    curl -s -X POST "$API" \
        -H "Content-Type: application/json" \
        -d "{\"message\": \"$message\"}" | python3 -m json.tool 2>/dev/null || true
    echo ""
}

# ---------------------------------------------------------------------------
# Test Cases
# ---------------------------------------------------------------------------

# Edge Cases
send_chat "NULL_BEHAVIOR"   ""
send_chat "BLANK"           "   "

# Intent 1: Greeting
send_chat "GREET_VI"        "xin chào"
send_chat "GREET_EN"        "hello"

# Intent 2: Help
send_chat "HELP"            "giúp"
send_chat "HELP_EN"         "help"

# Intent 3: Best Sellers
send_chat "BEST_SELLERS"    "sản phẩm bán chạy"
send_chat "HOT"             "hot"

# Intent 4: Search - simple keyword
send_chat "SEARCH_SIMPLE"   "tìm áo thun"

# Intent 4: Search - with max price
send_chat "SEARCH_MAX_PRICE" "áo dưới 300k"

# Intent 4: Search - with min price
send_chat "SEARCH_MIN_PRICE" "quần trên 200k"

# Intent 4: Search - with color
send_chat "SEARCH_COLOR"    "áo màu đen"

# Intent 4: Search - complex
send_chat "SEARCH_COMPLEX"  "tôi muốn áo xanh dưới 500k"

# Edge case: no results expected (assuming no product matches)
send_chat "NO_RESULTS"      "xyzabc123khôngtontai"

echo ""
echo "======================================"
echo "All test messages sent!"
echo "======================================"

