import json
import os
import time
from pymongo import MongoClient
from vnexpress_crawler import crawl_vnexpress
from otofun_crawler import crawl_otofun

# === CẤU HÌNH ===
# Danh sách mục muốn lấy từ VnExpress
VN_CATEGORIES = [
    'thoi-su', 'kinh-doanh', 'giai-tri', 'the-thao', 
    'phap-luat', 'giao-duc', 'suc-khoe', 'doi-song', 
    'du-lich', 'khoa-hoc', 'so-hoa', 'oto-xe-may'
]

# Danh sách mục muốn lấy từ Otofun
OF_CATEGORIES = [
    'oto-xe-may',  # Box Kỹ thuật
    'kinh-doanh',  # Box TTTM
    'du-lich',     # Box Các chuyến đi
    'doi-song'     # Cafe Otofun
]

# Số lượng tin muốn lấy mỗi mục
LIMIT_VN = 30  
LIMIT_OF = 10  

# === KẾT NỐI MONGODB (CÓ XỬ LÝ LỖI) ===
try:
    client = MongoClient("mongodb://localhost:27017/", serverSelectionTimeoutMS=2000)
    db = client["newsspeech"]
    collection = db["news"]
    # Kiểm tra kết nối thử
    client.server_info()
    HAS_MONGO = True
    print("✅ [DB] Đã kết nối MongoDB thành công.")
except Exception as e:
    HAS_MONGO = False
    print(f"⚠️ [DB] Không tìm thấy MongoDB ({e}). Chế độ chỉ lưu file JSON.")

# === HÀM LƯU FILE JSON TRỰC TIẾP ===
def save_to_json(news_list):
    """Lưu trực tiếp list tin tức vào file data/all_news.json"""
    if not news_list:
        print("⚠️ [JSON] Không có tin nào để lưu.")
        return

    os.makedirs("data", exist_ok=True)
    file_path = "data/all_news.json"
    
    try:
        with open(file_path, "w", encoding="utf-8") as f:
            json.dump(news_list, f, ensure_ascii=False, indent=2)
        print(f"✅ [JSON] Đã xuất file: {file_path} ({len(news_list)} tin)")
    except Exception as e:
        print(f"❌ [JSON] Lỗi khi lưu file: {e}")

# === HÀM LƯU MONGODB ===
def push_to_mongodb(news_list):
    if not HAS_MONGO or not news_list:
        return

    try:
        # Xóa dữ liệu cũ (Clean start)
        collection.delete_many({}) 
        # Thêm dữ liệu mới
        collection.insert_many(news_list)
        print(f"✅ [DB] Đã lưu {len(news_list)} tin vào MongoDB local.")
    except Exception as e:
        print(f"❌ [DB] Lỗi khi ghi vào MongoDB: {e}")

# === LOGIC CHÍNH ===
def run_crawler():
    print("🚀 BẮT ĐẦU QUÁ TRÌNH CRAWL DỮ LIỆU TỔNG HỢP...")
    start_time = time.time()
    all_news_buffer = []

    # 1. Crawl VnExpress
    print(f"\n--- 1. CRAWLING VNEXPRESS (Max {LIMIT_VN} tin/mục) ---")
    for cat in VN_CATEGORIES:
        try:
            news = crawl_vnexpress(cat, limit=LIMIT_VN)
            all_news_buffer.extend(news)
            print(f"   -> {cat}: {len(news)} bài")
        except Exception as e:
            print(f"   -> Lỗi mục {cat}: {e}")

    # 2. Crawl Otofun
    print(f"\n--- 2. CRAWLING OTOFUN (Max {LIMIT_OF} tin/mục) ---")
    for cat in OF_CATEGORIES:
        try:
            news = crawl_otofun(cat, limit=LIMIT_OF, headless=True)
            all_news_buffer.extend(news)
            print(f"   -> {cat}: {len(news)} bài")
        except Exception as e:
            print(f"   -> Lỗi mục {cat}: {e}")

    # 3. Lưu trữ
    print(f"\n--- 3. LƯU TRỮ DỮ LIỆU ({len(all_news_buffer)} tổng tin) ---")
    
    # Ưu tiên 1: Lưu JSON ngay lập tức (Quan trọng nhất cho App)
    save_to_json(all_news_buffer)
    
    # Ưu tiên 2: Lưu MongoDB (Nếu có)
    push_to_mongodb(all_news_buffer)

    elapsed = time.time() - start_time
    print(f"\n🎉 HOÀN THÀNH TOÀN BỘ SAU {elapsed:.2f} GIÂY!")

if __name__ == "__main__":
    run_crawler()