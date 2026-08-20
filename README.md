# KirKeiba MVP 4 — 4.4.0

KirKeiba là minigame cược đua ngựa ảo cho Paper. Người chơi chọn loại vé, đọc chỉ số ngựa và điều kiện đường đua, mua vé bằng Vault Economy rồi nhận thưởng theo cơ chế pool có tài trợ từ server.

Plugin không cần dựng trường đua hoặc spawn ngựa thật. Kết quả được mô phỏng ở server từ chỉ số, lối chạy, điều kiện vòng và biến động ngẫu nhiên.

## Tính năng chính

- Năm loại vé: `WIN`, `PLACE`, `UMAREN`, `EXACTA`, `TRIFECTA`.
- GUI rương để chọn loại vé, chọn ngựa, xem payout dự kiến và xác nhận.
- Giá vé cố định cấu hình riêng cho từng loại cược.
- Pari-mutuel theo từng thị trường, có tiền tài trợ server, sàn và trần hệ số.
- Chuột phải vào loại vé để xem người mua, số vé, lựa chọn và tiền trong pool.
- Ngựa có `power`, `stamina`, `finish` và bốn lối chạy.
- Điều kiện ảo từng vòng: cự ly, mặt đường, tình trạng đường và thời tiết.
- Bossbar cho các trạng thái mở cược, khóa cược, đang đua và kết quả.
- Chế độ chạy liên tục hoặc nghỉ giữa các vòng; bossbar tự ẩn trong thời gian nghỉ.
- Wiki nhiều item trong GUI, lịch sử kết quả và thống kê cá nhân.
- SQLite lưu vòng, vé, payout, audit log; tự hoàn vé vòng dang dở.
- Lệnh admin có quyền riêng, backup trước reset và reset toàn bộ về vòng `#1`.
- Hầu hết title, material, slot, lore, sound và thông báo đều chỉnh được bằng YAML.

## Yêu cầu

- Paper tương thích API Minecraft `1.21` (dự án đang chạy với Paper 26.2).
- Java 25 cho bản build hiện tại.
- Vault.
- Một economy provider tương thích Vault, ví dụ EssentialsX Economy.

`sqlite-jdbc` được khai báo trong `plugin.yml` và Paper tự tải thư viện.

## Cài đặt

1. Cài Vault và economy provider.
2. Chép `KirKeiba-4.4.0.jar` vào `plugins/`.
3. Khởi động server một lần để sinh `plugins/KirKeiba/config.yml`.
4. Dừng server, chỉnh config rồi khởi động lại.
5. Dùng `/keiba` để mở GUI.

Không để nhiều JAR KirKeiba cùng lúc trong `plugins/`.

## Luồng một vòng đua

1. Plugin chọn và khóa điều kiện vòng.
2. Trạng thái `BETTING_OPEN`: người chơi được mua vé.
3. Trạng thái `BETTING_LOCKED`: không nhận vé mới.
4. Trạng thái `RUNNING`: server mô phỏng thứ tự về đích.
5. Trạng thái `RESULT`: tính hệ số cuối, trả thưởng và lưu SQLite.
6. Mở vòng mới ngay hoặc chuyển sang `WAITING` tùy `schedule.mode`.

Trong `WAITING`, bossbar bị ẩn. `/keiba` và `/keiba status` cho biết còn bao nhiêu giây mới mở vòng tiếp theo.

## Các loại vé

| Khóa | Tên hiển thị | Số ngựa chọn | Điều kiện thắng | Xét thứ tự |
|---|---|---:|---|---|
| `WIN` | Thắng | 1 | Ngựa đã chọn về nhất | Có |
| `PLACE` | Vào Top 3 | 1 | Ngựa đã chọn về hạng 1, 2 hoặc 3 | Không |
| `UMAREN` | Umaren | 2 | Đúng hai ngựa về nhất và nhì | Không |
| `EXACTA` | Exacta | 2 | Đúng hạng nhất rồi hạng nhì | Có |
| `TRIFECTA` | Trifecta | 3 | Đúng hạng nhất, nhì và ba | Có |

Mỗi loại vé là một thị trường độc lập. Tiền trong `WIN` không được dùng trả vé `PLACE`, `UMAREN`, `EXACTA` hoặc `TRIFECTA`.

Tắt một loại vé bằng:

```yaml
bet-types:
  TRIFECTA:
    enabled: false
```

Loại bị tắt sẽ biến mất khỏi GUI; các loại còn lại tự dồn vào `gui.bet-type-slots`.

## Cơ chế pool và hệ số thưởng

Hệ số tự nhiên:

```text
(tiền người chơi trong thị trường + tiền server tài trợ)
× (1 − house-edge)
÷ tổng tiền nằm trên đáp án thắng
```

Hệ số trả thưởng:

```text
clamp(hệ số tự nhiên, minimum-odds, maximum-odds)
```

Payout của một vé thắng:

```text
tiền cược của vé × hệ số trả thưởng
```

Payout đã bao gồm tiền vốn. Ví dụ cược `30.000$`, hệ số `x1,70` thì nhận tổng cộng `51.000$`, lợi nhuận ròng là `21.000$`.

### Cấu hình kinh tế

```yaml
economy:
  house-edge: 0.15
  max-tickets-per-player: 5

  house-seed-pool:
    WIN: 30000.0
    PLACE: 30000.0
    UMAREN: 50000.0
    EXACTA: 75000.0
    TRIFECTA: 100000.0

  minimum-odds:
    WIN: 1.20
    PLACE: 1.15
    UMAREN: 1.30
    EXACTA: 1.50
    TRIFECTA: 2.00

  maximum-odds:
    WIN: 10.0
    PLACE: 5.0
    UMAREN: 20.0
    EXACTA: 30.0
    TRIFECTA: 50.0
```

- `house-edge: 0.15`: giữ 15% trước khi chia pool.
- `house-seed-pool`: tiền ảo server thêm vào từng thị trường mỗi vòng; đây là money faucet.
- `minimum-odds`: sàn payout để người thắng không nhận hệ số quá thấp.
- `maximum-odds`: trần payout, tránh vé hiếm tạo quá nhiều tiền.
- `max-tickets-per-player`: giới hạn tổng số vé một người trong một vòng.

Khi kinh tế bị lạm phát, ưu tiên giảm `house-seed-pool` và `maximum-odds`. Khi ít người chơi và phần thưởng kém hấp dẫn, có thể tăng seed hoặc sàn hệ số một cách thận trọng.

Hệ số hiện trên GUI là dự kiến và có thể thay đổi đến khi khóa cược. Với `PLACE`, hệ số xem trước chỉ mang tính tham khảo vì phải đợi kết quả mới biết cả ba lựa chọn thắng và tổng winning pool thực tế.

## Giá vé cố định

Mỗi loại vé dùng mức `stake` được cấu hình riêng. Người chơi không nhập số tiền thủ công:

```yaml
bet-types:
  WIN:
    enabled: true
    stake: 50000.0
```

## GUI chi tiết pool

Tại hàng loại cược:

- Chuột trái: chọn loại cược.
- Chuột phải: mở rương chi tiết thị trường đó.

Rương pool hiển thị:

- Số người mua và tổng số vé.
- Tổng tiền thật người chơi đã cược.
- Tiền server tài trợ.
- Tổng pool dùng tính thưởng.
- Player head của từng người mua.
- ID, lựa chọn, tiền cược và hệ số tại thời điểm mua của từng vé.
- 36 người mỗi trang theo config mặc định, có trang trước/sau.

Thông tin này là công khai. Nếu server không muốn lộ lựa chọn trước khi khóa cược, cần chỉnh code hoặc tắt đường truy cập; bản 4.4.0 chưa có chế độ ẩn tên/lựa chọn theo trạng thái.

```yaml
gui:
  pool-details:
    title: '&0Pool {type} • Trang {page}/{pages}'
    size: 54
    summary-slot: 4
    back-slot: 49
    previous-slot: 48
    next-slot: 50
    player-slots: [9,10,11,12,13,14,15,16,17]
    player:
      material: PLAYER_HEAD
      name: '&e&l{player}'
      ticket-line: '&7#{ticket_id} &f{selection} &8• &e{stake} &8• &6x{ticket_odds}'
```

## Chỉ số ngựa và lối chạy

Điểm mỗi bước mô phỏng gồm:

```text
power × hệ số power × (1 − tiến độ)
+ stamina × hệ số stamina × 0,45
+ finish × hệ số finish × tiến độ
+ bonus style × hệ số style
+ phong độ và dao động ngẫu nhiên
```

- `power`: đóng góp mạnh ở đầu chặng rồi giảm dần.
- `stamina`: đóng góp ổn định trong toàn chặng.
- `finish`: đóng góp tăng dần về cuối chặng.

### Bốn style

| Style | Tên | Cách hoạt động |
|---|---|---|
| `NIGE` | Dẫn đầu | Bonus lớn đầu chặng, giảm dần về 0 |
| `SENKO` | Bám đầu | Mạnh quanh giai đoạn 35% đường đua |
| `SASHI` | Tăng tốc giữa | Không có bonus trước 45%, tăng dần sau đó |
| `OIKOMI` | Nước rút cuối | Bị trừ trước 70%, nhận bonus nước rút rất lớn cuối chặng |

`{style_modifier}` chỉ cho biết điều kiện vòng tăng/giảm phần bonus style. Ví dụ `+20%` không có nghĩa toàn bộ tốc độ ngựa tăng 20%.

## Điều kiện đường đua MVP 4

Mỗi vòng chọn một mục từ bốn nhóm:

- `distances`: cự ly.
- `surfaces`: mặt đường.
- `track-states`: tình trạng đường.
- `weather`: thời tiết.

`weight` là trọng số tương đối, không bắt buộc tổng bằng 100. Ví dụ hai mục có weight `25` và `75` sẽ xuất hiện xấp xỉ 25% và 75%.

Các multiplier:

- `1.10`: tăng thành phần tương ứng 10%.
- `1.00`: giữ nguyên.
- `0.90`: giảm 10%.
- `randomness-multiplier`: thay đổi độ bất ngờ.
- `style-multipliers`: chỉ nhân bonus style, không nhân toàn bộ điểm chạy.

Ví dụ cự ly ngắn:

```yaml
race-conditions:
  enabled: true
  distances:
    SPRINT:
      enabled: true
      display-name: 'Nước rút'
      meters: 1200
      weight: 25
      power-multiplier: 1.08
      stamina-multiplier: 0.94
      finish-multiplier: 0.98
      randomness-multiplier: 1.00
      style-multipliers:
        NIGE: 1.12
        SENKO: 1.06
        SASHI: 0.96
        OIKOMI: 0.90
```

Đặt `race-conditions.enabled: false` để dùng điều kiện trung tính.

## Cấu hình ngựa, tên và lore

Mỗi ngựa hỗ trợ:

```yaml
horses:
  - name: "Akatsuki"
    style: "NIGE"
    power: 78
    stamina: 66
    finish: 58
    material: RED_DYE
    display-name: '&c&l#{number} Akatsuki &4🔥'
    lore:
      - '&7Danh hiệu: &c&lTia Chớp Bình Minh'
      - '&7Lối chạy: &f{style}'
      - '&c⚔ Sức mạnh: &f{power}'
      - '&a❤ Thể lực: &f{stamina}'
      - '&d✦ Nước rút: &f{finish}'
      - '&7Ảnh hưởng style: &b{style_modifier}%'
      - '&6$ Hệ số thưởng: &ex{odds}'
      - '{selection_hint}'
```

Nếu ngựa không có `material`, `display-name` hoặc `lore`, plugin dùng giá trị chung trong `gui.horse`.

Thay đổi danh sách/chỉ số ngựa cần restart server. `/keiba admin reload` không tạo lại danh sách ngựa đang nằm trong bộ nhớ.

## Lịch chạy vòng

```yaml
schedule:
  mode: CONTINUOUS
  interval-seconds: 300
  betting-seconds: 180
  locked-seconds: 15
  running-seconds: 35
  result-seconds: 30
```

- `CONTINUOUS`: hết thời gian kết quả sẽ mở vòng mới ngay.
- `INTERVAL`: hết kết quả sẽ nghỉ `interval-seconds`.
- `betting-seconds`: thời gian mua vé.
- `locked-seconds`: thời gian khóa trước khi chạy.
- `running-seconds`: thời gian trạng thái đang đua.
- `result-seconds`: thời gian giữ kết quả.

## Bossbar

```yaml
bossbar:
  enabled: true
  style: SEGMENTED_20
  titles:
    betting-open: '&6Keiba #{race} &8| &e{meters}m {surface} &8| &a{seconds}s'
  colors:
    betting-open: GREEN
```

Style hợp lệ gồm `SOLID`, `SEGMENTED_6`, `SEGMENTED_10`, `SEGMENTED_12`, `SEGMENTED_20`. Bossbar luôn ẩn trong `WAITING`, kể cả khi còn khóa `waiting` cũ trong config.

## Âm thanh

```yaml
sounds:
  open: BLOCK_NOTE_BLOCK_PLING
  lock: BLOCK_IRON_DOOR_CLOSE
  start: ENTITY_HORSE_GALLOP
  result: UI_TOAST_CHALLENGE_COMPLETE
```

Dùng tên Bukkit Sound hợp lệ. Xóa khóa, để trống hoặc đặt `NONE`/`OFF` để tắt riêng âm thanh đó.

## Wiki và GUI

`gui.help.pages` là danh sách item Wiki. Mỗi item có `slot`, `material`, `name`, `lore`. Có thể thêm/bớt trang mà không sửa Java.

Các slot bắt đầu từ 0. Kích thước inventory phải là 9, 18, 27, 36, 45 hoặc 54; plugin chuẩn hóa giá trị về một kích thước hợp lệ tối đa 54.

Material phải là tên Minecraft/Bukkit hợp lệ như `BOOK`, `CLOCK`, `PLAYER_HEAD`, `RED_DYE`. Material sai sẽ dùng vật phẩm dự phòng.

## Placeholder

| Nhóm | Placeholder |
|---|---|
| Vòng | `{race}`, `{next_race}`, `{seconds}`, `{state}`, `{pool}` |
| Điều kiện | `{distance}`, `{meters}`, `{surface}`, `{track_state}`, `{weather}` |
| Ngựa | `{number}`, `{name}`, `{style}`, `{power}`, `{stamina}`, `{finish}` |
| Dự đoán | `{style_modifier}`, `{odds}` |
| Vé | `{type}`, `{required}`, `{stake}`, `{payout}`, `{description}`, `{example}`, `{house_seed}` |
| Pool GUI | `{tickets}`, `{players}`, `{player_pool}`, `{market_pool}`, `{page}`, `{pages}` |
| Người mua | `{player}`, `{player_tickets}`, `{player_total}` |
| Dòng vé | `{ticket_id}`, `{selection}`, `{ticket_odds}` và `{stake}` |
| Kết quả | `{first}`, `{second}`, `{first_number}`, `{second_number}` |

Placeholder không hợp lệ hoặc thiếu dữ liệu được render thành `—`. Chuỗi hỗ trợ mã màu legacy `&`, ví dụ `&a`, `&e`, `&7`, `&l`.

## Lệnh người chơi

| Lệnh | Chức năng |
|---|---|
| `/keiba` | Mở GUI hoặc báo countdown khi đang nghỉ |
| `/keiba status` | Xem vòng, trạng thái, thời gian và điều kiện |
| `/keiba tickets` | Liệt kê vé của người chơi trong vòng hiện tại |
| `/keiba help` | Mở Wiki và in trợ giúp |

## Lệnh quản trị

| Lệnh | Chức năng |
|---|---|
| `/keiba admin help` | Danh sách lệnh admin |
| `/keiba admin forcestart` | Khóa cược ngay nếu đang mở |
| `/keiba admin cancel` | Hủy vòng, hoàn vé và mở vòng mới |
| `/keiba admin reload` | Nạp lại config; danh sách ngựa vẫn cần restart |
| `/keiba admin reset current confirm` | Hoàn vé và tạo lại vòng hiện tại |
| `/keiba admin reset history confirm` | Xóa Top 3 khỏi lịch sử hiển thị |
| `/keiba admin reset stats confirm` | Xóa vé đã settle và thống kê người chơi |
| `/keiba admin reset all confirm` | Xóa toàn bộ dữ liệu, reset ID và mở lại từ vòng `#1` |

Mọi reset đều tạo backup SQLite trước khi thay đổi dữ liệu.

## Quyền

| Permission | Mặc định | Ý nghĩa |
|---|---|---|
| `kirkeiba.use` | `true` | Permission khai báo cho tích hợp/quản lý quyền |
| `kirkeiba.bet` | `true` | Permission khai báo cho tích hợp/quản lý quyền |
| `kirkeiba.admin` | `op` | Quyền cha cho toàn bộ admin |
| `kirkeiba.admin.forcestart` | `op` | Ép khóa cược |
| `kirkeiba.admin.cancel` | `op` | Hủy vòng |
| `kirkeiba.admin.reload` | `op` | Reload config |
| `kirkeiba.admin.reset` | `op` | Reset dữ liệu |

Handler admin hiện yêu cầu cả `kirkeiba.admin` và quyền con tương ứng. Bản 4.4.0 mới chỉ khai báo `kirkeiba.use` và `kirkeiba.bet` trong `plugin.yml`, chưa chặn command/mua vé theo hai quyền này.

## Dữ liệu, backup và khôi phục

- Database: `plugins/KirKeiba/keiba.db`.
- Backup: `plugins/KirKeiba/backups/keiba-<timestamp>.db`.
- Lưu vòng, Top 3, tổng pool, payout, vé và audit log.
- Nếu server tắt khi vòng chưa settle, plugin hoàn các vé chưa settle khi khởi động lại.
- `reset all` xóa dữ liệu và `sqlite_sequence` của `races`, `tickets`, `audit_log`; vòng mới bắt đầu từ `#1`.

Không xóa database khi server đang chạy. Nên backup cả thư mục `plugins/KirKeiba/` trước khi nâng cấp lớn.

## Reload hay restart?

| Thay đổi | Reload | Restart đề xuất |
|---|---|---|
| Text, lore, title, slot, material | Có | Không bắt buộc |
| Sound | Có | Không bắt buộc |
| Stake, seed, min/max odds | Có | Nên áp dụng trước vòng mới |
| Schedule | Có | Deadline đang chạy không được tính lại |
| Điều kiện đường đua | Có | Áp dụng khi mở vòng tiếp theo |
| Danh sách, style, chỉ số ngựa | Không đầy đủ | Có |

## Build từ source

Windows PowerShell:

```powershell
$env:JAVA_HOME = 'đường-dẫn-tới-JDK-25'
.\gradlew.bat clean test build --no-daemon
```

JAR nằm trong:

```text
build/libs/KirKeiba-4.4.0.jar
```

## Xử lý lỗi thường gặp

### `Thiếu cấu hình horses`

Kiểm tra `horses:` có ít nhất ba ngựa và YAML đúng thụt dòng. Style phải là `NIGE`, `SENKO`, `SASHI`, `OIKOMI` viết hoa.

### Không có economy

Kiểm tra Vault và economy provider đã enable trước KirKeiba.

### Hệ số khác lúc mua

Đây là cơ chế pool: người khác có thể mua thêm vé trước khi khóa. Hệ số cuối được tính tại kết quả và chịu sàn/trần config.

### Thắng nhưng số tiền nhận không giống lợi nhuận

Payout đã gồm vốn. Lợi nhuận ròng bằng `payout − stake`.

### Config mới không tự xuất hiện

Plugin cố ý không ghi đè config đang dùng để tránh làm mất tùy chỉnh và các sound đã xóa. So sánh `src/main/resources/config.yml` với file live rồi chèn khóa mới thủ công.

## Phạm vi hiện tại

- Đây là minigame GUI; `visual-track` được giữ để tương thích nhưng chưa dựng trường đua vật lý.
- Kết quả không học từ lịch sử cũ; mỗi vòng được mô phỏng độc lập.
- Không có mùa giải, bảng xếp hạng toàn server hoặc NPC ngựa vật lý.
- GUI pool hiện công khai vé ngay cả khi vòng chưa khóa.
