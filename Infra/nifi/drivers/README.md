# Thư mục driver cho NiFi (Tuấn)

Đặt file MySQL JDBC driver (vd `mysql-connector-j-8.x.x.jar`) vào thư mục này.
Container NiFi thấy nó tại `/opt/nifi/nifi-current/drivers/`; khai báo đường dẫn đó trong DBCPConnectionPool.
File `.jar` không commit lên Git (mỗi người tự tải).
