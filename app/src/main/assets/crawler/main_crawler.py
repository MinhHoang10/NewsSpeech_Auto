import json
import os
from pymongo import MongoClient
from vnexpress_crawler import crawl_vnexpress
from otofun_crawler import crawl_otofun

# === KẾT NỐI LOCAL MONGODB ===
client = MongoClient("mongodb://localhost:27017/")
db = client["newsspeech"]
collection = db["newss"]

def push_to_mongodb(news_list):
    if news_list:
        # Xóa dữ liệu cũ
        collection.delete_many({})
        # Thêm dữ liệu mới
        collection.insert_many(news_list)
        print(f"Đã push {len(news_list)} tin vào MongoDB local.")
    else:
        print("Không có tin để push.")

def export_to_json():
    # Export toàn bộ collection → JSON
    all_news = list(collection.find({}, {"_id": 0}))  # Loại bỏ _id
    os.makedirs(r"D:\NewsSpeechAuto\app\src\main\assets", exist_ok=True)
    with open(r"D:\NewsSpeechAuto\app\src\main\assets/all_news.json", "w", encoding="utf-8") as f:
        json.dump(all_news, f, ensure_ascii=False, indent=2)
    print(f"Đã export JSON: all_news.json")

def run_crawler():
    print("Bắt đầu crawl & lưu vào MongoDB local...")

    # Crawl
    vn_news = crawl_vnexpress('thoi-su', 5) + crawl_vnexpress('oto-xe-may', 3)
    oto_news = crawl_otofun(3)
    all_news = vn_news + oto_news

    # Push vào MongoDB
    push_to_mongodb(all_news)

    # Export ra JSON cho Android
    export_to_json()

    print(f"HOÀN THÀNH! {len(all_news)} tin đã được lưu.")

if __name__ == "__main__":
    run_crawler()