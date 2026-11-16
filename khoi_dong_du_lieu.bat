@echo off
chcp 65001 >nul
echo =========================================================================
echo   VN SPORTS SOA - NẠP DỮ LIỆU TỰ ĐỘNG (5 MÔN / 22 ĐỘI / 20 TRẬN ĐẤU)
echo   - Bóng đá: 6 đội (11 cầu thủ ĐTQG mỗi đội)
echo   - 4 môn còn lại: đúng 4 đội mỗi môn
echo =========================================================================
python seed_all_soa_data.py
echo.
echo =========================================================================
echo   ĐÃ NẠP XONG TOÀN BỘ DỮ LIỆU HỆ THỐNG VN SPORTS SOA!
echo   Vui lòng mở trình duyệt tại: http://localhost:8080
echo =========================================================================
pause
