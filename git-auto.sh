#!/bin/bash

echo "📥 Menarik pembaruan dari GitHub..."
git pull origin main

echo "📦 Menambahkan semua perubahan..."
git add .

echo "📝 Masukkan pesan commit:"
read commit_msg

git commit -m "$commit_msg"

echo "🚀 Mengirim ke GitHub..."
git push origin main

echo "✅ Selesai!"
