# otofun_crawler.py
import json
import time
import re
from datetime import datetime
from selenium import webdriver
from selenium.webdriver.common.by import By 
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager
from bs4 import BeautifulSoup

def setup_driver(headless=True):
    options = Options()
    if headless: options.add_argument('--headless')
    options.add_argument('--no-sandbox')
    options.add_argument('--disable-blink-features=AutomationControlled')
    options.add_argument('user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36')
    service = Service(ChromeDriverManager().install())
    return webdriver.Chrome(service=service, options=options)

def crawl_otofun(category='oto-xe-may', limit=20, headless=True):
    category_map = {
        'oto-xe-may': 'https://www.otofun.net/forums/oto-xe-may.2/',
        'kinh-doanh': 'https://www.otofun.net/forums/tttm-xe-co.292/',
        'bat-dong-san': 'https://www.otofun.net/forums/bat-dong-san.77/',
        'doi-song': 'https://www.otofun.net/forums/cafe-otofun.16/',
        'giai-tri': 'https://www.otofun.net/forums/cafe-otofun.16/',
        'the-thao': 'https://www.otofun.net/forums/van-hoa-the-thao.163/',
        'du-lich': 'https://www.otofun.net/forums/cac-chuyen-di.24/'
    }

    url = category_map.get(category, category_map['doi-song'])
    print(f"[Otofun] Bắt đầu crawl '{category}'...")

    driver = setup_driver(headless)
    news_list = []
    
    try:
        driver.get(url)
        time.sleep(3)

        while len(news_list) < limit:
            soup = BeautifulSoup(driver.page_source, 'html.parser')
            threads = soup.find_all('div', class_='structItem-title')
            
            if not threads: break

            for item in threads:
                if len(news_list) >= limit: break

                a_tag = item.find('a', href=re.compile(r'/threads/'))
                if not a_tag: continue
                
                title = a_tag.get_text(strip=True)
                link = "https://www.otofun.net" + a_tag['href']
                
                if any(n['link'] == link for n in news_list): continue
                article_id = re.search(r'\.(\d+)/?$', link).group(1) if re.search(r'\.(\d+)/?$', link) else str(hash(link))[-8:]

                # --- VÀO CHI TIẾT ---
                content = ""
                img_url = None
                try:
                    driver.execute_script("window.open('');")
                    driver.switch_to.window(driver.window_handles[-1])
                    driver.get(link)
                    time.sleep(2) # Tăng thời gian chờ lên chút để chắc chắn load hết text

                    detail_soup = BeautifulSoup(driver.page_source, 'html.parser')
                    first_post = detail_soup.find('article', class_='message--post')
                    
                    if first_post:
                        body = first_post.find('div', class_='bbWrapper')
                        if body:
                            # Lấy ảnh
                            img_tag = body.find('img', class_='bbImage') or body.find('img')
                            if img_tag and img_tag.get('src'):
                                img_url = img_tag['src']
                                if not img_url.startswith('http'): img_url = "https://www.otofun.net" + img_url
                            
                            # --- SỬA Ở ĐÂY: LẤY TOÀN BỘ TEXT ---
                            # separator='\n' giúp giữ xuống dòng, dễ đọc hơn
                            # Bỏ đoạn [:400] đi
                            content = body.get_text(separator='\n', strip=True)
                    
                    if not content: content = "Xem chi tiết tại diễn đàn."
                    
                    driver.close()
                    driver.switch_to.window(driver.window_handles[0])

                except Exception as e:
                    print(f"   [Lỗi bài] {str(e)[:50]}")
                    try: 
                        if len(driver.window_handles) > 1:
                            driver.close()
                            driver.switch_to.window(driver.window_handles[0])
                    except: pass
                    content = "Lỗi tải nội dung."

                news_list.append({
                    "id": article_id,
                    "title": title,
                    "content": content, # <-- Full text
                    "image": img_url,
                    "link": link,
                    "timestamp": datetime.now().isoformat(),
                    "source": "Otofun",
                    "category": category
                })
                print(f"      + [Otofun] Đã lấy: {title[:20]}... ({len(content)} chars)")

            if len(news_list) >= limit: break
            
            try:
                next_btn = driver.find_element(By.CSS_SELECTOR, 'a.pageNav-jump--next')
                if next_btn:
                    next_btn.click()
                    time.sleep(3)
                else: break
            except: break

    except Exception as e:
        print(f"[Otofun] Lỗi Critical: {e}")
    finally:
        driver.quit()

    return news_list