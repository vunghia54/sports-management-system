# BÁO CÁO KẾT QUẢ KIỂM THỬ POSTMAN — TH1 & TH3 (ATHLETE CRUD & REST NÂNG CAO)

**Hệ thống:** Sports Management System  
**Ngày thực hiện:** 18/11/2026  
**Bộ kiểm thử:** [`tests/postman/th1-th3-crud.postman_collection.json`](th1-th3-crud.postman_collection.json)  
**Kết quả xuất Newman:** [`docs/test/test-results/th1-th3-crud-newman.json`](../../docs/test/test-results/th1-th3-crud-newman.json)  
**Tổng hợp lượt chạy:** **21/21 requests, 21/21 assertions — 0 failed** (thời gian chạy `2.9 s`, phản hồi trung bình `56 ms`, `runTag = TH13_37939699`).

## Cách chạy lại

```powershell
npx newman run tests/postman/th1-th3-crud.postman_collection.json -r cli,json --reporter-json-export docs/test/test-results/th1-th3-crud-newman.json
```

---

## Bảng kết quả kiểm thử chi tiết

| Test case | Request | Expected | Actual | Kết quả |
|---|---|---|---|---|
| **0** | `POST /api/v1/auth/login` (`manager1`) | `200 OK`, trả về `accessToken` JWT | `200 OK` (`118 ms`), lưu `managerToken` | **PASS** |
| **0b** | `POST /api/v1/auth/login` (`admin`) | `200 OK`, trả về `accessToken` JWT cho thao tác `DELETE` | `200 OK` (`78 ms`), lưu `adminToken` | **PASS** |
| **1 [TH1]** | `POST /api/v1/athletes` (tạo VĐV A — `MALE`) | `201 Created`, trả `id` và `fullName` chứa `AAA` | `201 Created` (`id = 258`, `32 ms`) | **PASS** |
| **2 [TH1]** | `GET /api/v1/athletes/258` | `200 OK`, đúng `id` và `identityCardNumber` của chủ sở hữu | `200 OK` (`id = 258`, khớp CCCD, `18 ms`) | **PASS** |
| **3 [TH1]** | `GET /api/v1/athletes` | `200 OK`, danh sách JSON mảng chứa VĐV `#258` | `200 OK` (mảng chứa `id = 258`, `87 ms`) | **PASS** |
| **4 [TH1]** | `PUT /api/v1/athletes/258` | `200 OK`, cập nhật toàn bộ thông tin (`AAA Updated ...`) | `200 OK` (`fullName` chứa `AAA Updated`, `44 ms`) | **PASS** |
| **5 [TH3]** | `PATCH /api/v1/athletes/258` (`{"healthStatus":"INJURED"}`) | `200 OK`, cập nhật một phần `healthStatus`, giữ nguyên `fullName` | `200 OK` (`healthStatus = INJURED`, giữ `AAA Updated`, `34 ms`) | **PASS** |
| **6 [Setup]** | `POST /api/v1/athletes` (tạo VĐV B — `MALE`) | `201 Created` | `201 Created` (`id = 259`, `17 ms`) | **PASS** |
| **7 [Setup]** | `POST /api/v1/athletes` (tạo VĐV C — `FEMALE`) | `201 Created` | `201 Created` (`id = 260`, `22 ms`) | **PASS** |
| **8 [TH3]** | `POST /api/v1/athletes` (thiếu `fullName`) | `400 Bad Request`, cấu trúc lỗi thống nhất `{timestamp, status, error, path, errors.fullName}` | `400 Bad Request` (đủ 5 trường + `errors.fullName`, `50 ms`) | **PASS** |
| **9 [TH3]** | `POST /api/v1/athletes` (`gender = INVALID_GENDER`) | `400 Bad Request`, thông báo lỗi rõ ràng | `400 Bad Request` (`status = 400`, có `message`, `22 ms`) | **PASS** |
| **10 [TH3]** | `GET /api/v1/athletes/999999999` | `404 Not Found` (`status = 404`) | `404 Not Found` (`status = 404`, `17 ms`) | **PASS** |
| **11 [TH3]** | `GET /api/v1/athletes/paged?keyword=TH13_37939699&page=0&size=2` | `200 OK`, `content.length = 2`, kèm metadata `page=0, size=2, totalElements>=3, totalPages>=2` | `200 OK` (`content.length = 2`, `totalElements = 3`, `totalPages = 2`, `26 ms`) | **PASS** |
| **12 [TH3]** | `GET /api/v1/athletes/paged?keyword=TH13_37939699&gender=MALE&page=0&size=10` | `200 OK`, mọi phần tử có `gender = MALE` | `200 OK` (`2` bản ghi đều `gender = MALE`, `17 ms`) | **PASS** |
| **13 [TH3]** | `GET /api/v1/athletes?keyword=TH13_37939699&sortBy=fullName&direction=desc` | `200 OK`, sắp xếp giảm dần theo `fullName` (`CCC` → `BBB` → `AAA`) | `200 OK` (đúng thứ tự `CCC`, `BBB`, `AAA`, `18 ms`) | **PASS** |
| **13b [TH3]** | `GET /api/v1/athletes/paged?name=TH13_37939699&gender=MALE&page=0&size=5&sortBy=fullName&direction=asc` | `200 OK`, kết hợp tìm theo `name` + lọc `MALE` + sắp xếp `fullName asc` (`AAA` → `BBB`) + metadata trang (`page=0, size=5, totalElements=2, totalPages=1`) | `200 OK` (`2` phần tử `MALE` đúng `name`, xếp `AAA` trước `BBB`, đủ metadata trang, `15 ms`) | **PASS** |
| **14 [TH3]** | `GET /api/v1/athletes/paged?sortBy=nonExistentColumn` | `400 Bad Request` (chặn trường sắp xếp ngoài danh sách trắng) | `400 Bad Request` (`status = 400`, `10 ms`) | **PASS** |
| **15 [TH1]** | `DELETE /api/v1/athletes/258` | `204 No Content` | `204 No Content` (`440 ms`) | **PASS** |
| **16 [TH1]** | `GET /api/v1/athletes/258` (sau khi xóa) | `404 Not Found` | `404 Not Found` (`18 ms`) | **PASS** |
| **17 [Cleanup]** | `DELETE /api/v1/athletes/259` | `204 No Content` | `204 No Content` (`57 ms`) | **PASS** |
| **18 [Cleanup]** | `DELETE /api/v1/athletes/260` | `204 No Content` | `204 No Content` (`49 ms`) | **PASS** |
