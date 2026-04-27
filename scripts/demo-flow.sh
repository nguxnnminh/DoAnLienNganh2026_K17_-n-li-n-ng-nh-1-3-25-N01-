#!/bin/bash
# ============================================
# 🎬 CLOTHING STORE - P3 DEMO FLOW
# ============================================
# Các bước: Register → Login → Add to cart → Checkout → Recommendation → Chatbot
#
# Cách chạy:
#   bash scripts/demo-flow.sh
# ============================================

BASE_URL="http://localhost:8080/api"
HEADERS="Content-Type: application/json"

echo "=========================================="
echo "🚀 CLOTHING STORE - P3 DEMO FLOW"
echo "=========================================="

# -------------------------------------------------
# 1. REGISTER
# -------------------------------------------------
echo ""
echo "📌 [1/6] Register new user..."
REGISTER_RESPONSE=$(curl -s -X POST "${BASE_URL}/auth/register" \
  -H "$HEADERS" \
  -d '{
    "fullName": "Demo User",
    "email": "demo@example.com",
    "password": "123456",
    "phone": "0909123456"
  }')
echo "Response: $REGISTER_RESPONSE"

# -------------------------------------------------
# 2. LOGIN (JWT)
# -------------------------------------------------
echo ""
echo "📌 [2/6] Login to get JWT token..."
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "$HEADERS" \
  -d '{
    "email": "demo@example.com",
    "password": "123456"
  }')
echo "Response: $LOGIN_RESPONSE"

# Extract token (basic grep/sed)
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"$//')
echo "JWT Token: ${TOKEN:0:40}..."

AUTH_HEADER="Authorization: Bearer $TOKEN"

# -------------------------------------------------
# 3. ADD TO CART
# -------------------------------------------------
echo ""
echo "📌 [3/6] Add product to cart (variantId=1, qty=2)..."
CART_RESPONSE=$(curl -s -X POST "${BASE_URL}/cart/add" \
  -H "$HEADERS" \
  -H "$AUTH_HEADER" \
  -d '{
    "variantId": 1,
    "quantity": 2
  }')
echo "Response: $CART_RESPONSE"

# -------------------------------------------------
# 4. CHECKOUT
# -------------------------------------------------
echo ""
echo "📌 [4/6] Checkout cart..."
CHECKOUT_RESPONSE=$(curl -s -X POST "${BASE_URL}/orders/checkout" \
  -H "$HEADERS" \
  -H "$AUTH_HEADER" \
  -d '{
    "customerName": "Demo User",
    "address": "123 Demo Street, Hanoi",
    "phone": "0909123456"
  }')
echo "Response: $CHECKOUT_RESPONSE"

# -------------------------------------------------
# 5. CALL RECOMMENDATION
# -------------------------------------------------
echo ""
echo "📌 [5/6] Get personalized recommendations..."
REC_RESPONSE=$(curl -s -X GET "${BASE_URL}/recommendations?limit=8" \
  -H "$AUTH_HEADER")
echo "Response: $REC_RESPONSE" | head -c 800
echo ""

echo ""
echo "📌 [5b/6] Get similar products for product id=1..."
SIMILAR_RESPONSE=$(curl -s -X GET "${BASE_URL}/products/1/similar?limit=6" \
  -H "$AUTH_HEADER")
echo "Response: $SIMILAR_RESPONSE" | head -c 800
echo ""

# -------------------------------------------------
# 6. CALL CHATBOT
# -------------------------------------------------
echo ""
echo "📌 [6/6] Chatbot query..."
CHAT_RESPONSE=$(curl -s -X POST "${BASE_URL}/chatbot" \
  -H "$HEADERS" \
  -d '{
    "message": "tôi muốn áo màu đen dưới 500k"
  }')
echo "Response: $CHAT_RESPONSE" | head -c 800
echo ""

# -------------------------------------------------
# BONUS: ANALYTICS
# -------------------------------------------------
echo ""
echo "📌 [BONUS] Analytics..."
echo "--- Top Products ---"
curl -s -X GET "${BASE_URL}/analytics/top-products?limit=5" | head -c 600
echo ""

echo ""
echo "--- Trending ---"
curl -s -X GET "${BASE_URL}/analytics/trending?limit=5" | head -c 600
echo ""

echo ""
echo "--- Overview ---"
curl -s -X GET "${BASE_URL}/analytics/overview" | head -c 600
echo ""

# -------------------------------------------------
echo ""
echo "=========================================="
echo "✅ DEMO FLOW COMPLETE!"
echo "=========================================="

