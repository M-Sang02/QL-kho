# QL-kho

Ứng dụng quản lý kho Android + Web, giao diện Material 3 pha Glass/Blur.

## Công nghệ
- Kotlin + Jetpack Compose + Material 3
- Firebase Authentication (Anonymous sync; Google Sign-In deferred)
- Firestore (đồng bộ bản ghi theo phiên bản, có thể khôi phục từ lịch sử thay đổi)
- Room (lưu sản phẩm, tồn kho, lô, đơn hàng và thanh toán trên thiết bị)
- WorkManager (đồng bộ khi có mạng và định kỳ)
- GitHub Actions

## Mở bằng Android Studio / AndroidIDE
1. Mở thư mục repository.
2. Chờ Gradle sync; chạy tác vụ `:app:assembleDebug`.
3. Tạo Firebase project, đăng ký ứng dụng Android với package `com.kho.glass`, rồi bật **Anonymous** trong Firebase Authentication để đồng bộ bằng mã kho không cần Google.
4. Đặt `google-services.json` vào `app/`. OAuth Android/Web chỉ cần khi Google Sign-In được bật lại; đồng bộ ẩn danh không cần các OAuth client này.
5. Tạo Firestore database rồi triển khai `firestore.rules` bằng Firebase CLI.
6. Chạy `:app:assembleDebug`.

## Google Login

Google Sign-In được tạm hoãn trong giai đoạn này. Đồng bộ qua mã dùng Firebase Anonymous Authentication nên không cần OAuth, SHA fingerprint hay Google Play Services. Ứng dụng mặc định mở kho ngoại tuyến; chạm tài khoản ở đầu menu để bật đồng bộ qua mã. Khi hoàn thiện lại Google Sign-In, đăng ký SHA-1/SHA-256 trong `QL-kho-Firebase-SHA.txt`, bật Google provider và tải lại `google-services.json` có OAuth Web client vào `app/`.

### Cố định khóa ký Android

Debug và release dùng hai khóa PKCS12 riêng. Không commit file `signing/*.p12` hoặc `local.properties`. Mẫu cấu hình local (điền các mật khẩu và alias riêng của bạn):

```properties
QL_KHO_DEBUG_STORE_FILE=signing/ql-kho-debug.p12
QL_KHO_DEBUG_STORE_PASSWORD=<debug-store-password>
QL_KHO_DEBUG_KEY_ALIAS=ql-kho-debug
QL_KHO_DEBUG_KEY_PASSWORD=<same-as-debug-store-password>
QL_KHO_RELEASE_STORE_FILE=signing/ql-kho-release.p12
QL_KHO_RELEASE_STORE_PASSWORD=<release-store-password>
QL_KHO_RELEASE_KEY_ALIAS=ql-kho-release
QL_KHO_RELEASE_KEY_PASSWORD=<same-as-release-store-password>
```

Gradle ưu tiên biến môi trường có cùng tên rồi mới đọc `local.properties`. Vì khóa là PKCS12, đặt `KEY_PASSWORD` giống `STORE_PASSWORD`. Workflow debug hiện không cần GitHub Secrets ký và dùng khóa debug mặc định của runner để APK có thể build trong lúc Google Sign-In được tạm hoãn. Khóa này không có SHA cố định giữa các runner và không dùng được cho Google Sign-In. Khi bật lại Google Sign-In, cấu hình các secrets `QL_KHO_DEBUG_KEYSTORE_BASE64`, `QL_KHO_DEBUG_STORE_PASSWORD`, `QL_KHO_DEBUG_KEY_ALIAS`, `QL_KHO_DEBUG_KEY_PASSWORD` cùng nhóm `QL_KHO_RELEASE_*`; Base64 hóa từng keystore PKCS12 và lưu secrets riêng tư.

Trên PowerShell, lấy giá trị Base64 để lưu trực tiếp thành GitHub Actions secret (không đưa vào mã nguồn):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("signing/ql-kho-debug.p12"))
[Convert]::ToBase64String([IO.File]::ReadAllBytes("signing/ql-kho-release.p12"))
```

Lấy các giá trị password/alias từ `local.properties` để lưu vào GitHub Secrets tương ứng. Không chia sẻ hoặc commit `local.properties` và file `.p12`; giữ một bản sao lưu an toàn của khóa release vì mất khóa sẽ ngăn cập nhật APK đã phát hành.

## Đồng bộ và chia sẻ kho

Room vẫn là nguồn dữ liệu để đọc/ghi ngoại tuyến. Để bật đồng bộ, trong Firebase Console mở **Authentication → Sign-in method → Anonymous** và bật nhà cung cấp này; bảo đảm đã tạo Firestore database và triển khai `firestore.rules`. Trong app, chạm tài khoản ở đầu menu → **Bật đồng bộ qua mã (không cần Google)**; thiết bị phải có Internet lúc kết nối lần đầu. Chọn giữ kho hiện tại, sau đó vào **Cài đặt → Tên kho**, bật **Chia sẻ kho bằng mã** rồi tạo mã hoặc nhập lại mã cũ. Thiết bị tham gia nhập mã trong **Quản lý kho → Nhập mã**; đây là yêu cầu tham gia, chủ kho phải duyệt trước khi đồng bộ dữ liệu. Từ đó WorkManager đồng bộ khi có mạng các sản phẩm, đơn vị, lô, biến động, đơn hàng, dòng đơn, allocations FIFO và thanh toán lên `warehouses/{warehouseId}/changes`. Mỗi phiên bản cloud là một bản ghi bất biến; các thiết bị hợp nhất ledger biến động/tồn kho và thanh toán thay vì thay toàn bộ kho bằng một snapshot. Tắt hoặc đổi mã sẽ ngừng lời mời mới nhưng không xóa mã cũ, thành viên hay dữ liệu. Thành viên đã duyệt còn quyền cho tới khi mã bị tắt ít nhất 30 ngày và thiết bị không đồng bộ ít nhất 30 ngày; sau đó họ cần nhập lại mã và chờ duyệt. Khôi phục cùng mã trước khi quyền hết hạn giữ nguyên liên kết thành viên. Tài khoản ẩn danh gắn với thiết bị; không xóa dữ liệu ứng dụng cho tới khi có luồng khôi phục/liên kết tài khoản.

Mỗi sản phẩm hỗ trợ một ảnh chính và tối đa 10 ảnh thêm trong một danh sách chung. Có thể chọn nhiều ảnh từ thư viện hoặc chụp từng ảnh; ảnh đầu danh sách là ảnh chính. Nhấn giữ một ảnh 1 giây rồi kéo để đổi thứ tự; nút X xóa ảnh ngay cả khi đó là ảnh chính. Ảnh lưu cục bộ trên thiết bị; đường dẫn tệp không được đưa lên Firestore. ZIP sao lưu sản phẩm bao gồm ảnh. Hệ thống phân quyền hiện hỗ trợ chủ kho/thành viên và hạn chế đổi mã cho thành viên; chưa có màn hình chủ kho thu hồi riêng từng thành viên.

Ẩn toàn bộ sản phẩm sẽ lưu ngày ẩn; sản phẩm và ảnh được tự động xóa sau 30 ngày (WorkManager dọn định kỳ, đồng thời kiểm tra khi mở kho). Mục Lịch sử ghi nhận ẩn, khôi phục và xóa bằng biểu tượng riêng; phần mở rộng của sản phẩm đã ẩn hiển thị số ngày còn lại. Có thể chọn **Ẩn một phần tồn kho**, chọn đơn vị Cái/Kg và nhập số lượng; thao tác này giảm tồn kho nhưng vẫn giữ sản phẩm hiển thị.

**Chia sẻ xung quanh** trong Cài đặt mở Android Sharesheet, qua đó có thể dùng Quick Share để chuyển mã mời giữa các thiết bị. Nút **Nhận mã** mở ô nhập/dán mã; khi chọn ứng dụng này từ Quick Share, mã nhận được được điền sẵn. Đây chỉ là cách chuyển mã, không đồng bộ dữ liệu kho khi ngoại tuyến: việc đồng bộ vẫn cần Internet. ZIP hiện có chỉ sao lưu sản phẩm, số lượng, giá và ảnh; không dùng ZIP đó để thay thế đồng bộ đơn hàng/lịch sử.

## GitHub Actions
Workflow `.github/workflows/android.yml` build debug APK và lưu artifact.

## Trạng thái
Android hiện có lưu dữ liệu cục bộ bằng Room và các luồng cơ bản: tạo/sửa/ẩn/khôi phục/xóa hẳn sản phẩm, thêm ảnh từ thư viện hoặc camera, tìm kiếm trên tab riêng và lọc hàng sắp hết; thông báo hàng sắp hết và báo cáo nằm trong nút thông báo. Xóa hẳn giữ dòng chữ trong đơn cũ nhưng bỏ ảnh. Nếu trùng tên và đơn vị khi thêm thủ công, tồn đầu kỳ được cộng thành lô mới vào sản phẩm có sẵn. Tạo sản phẩm có đơn vị, tồn đầu kỳ và giá nhập/bán; nhập tồn theo lô; bán FIFO; thanh toán từng phần; hủy đơn và xác nhận hoàn tiền. Các tab Kho, Lịch sử và Bán hàng có thể chuyển bằng cách vuốt ngang.

Tên ứng dụng hiển thị trên Android là “Quản lý kho”, với biểu tượng kho riêng.

Danh sách kho dùng lưới hai cột; kéo xuống ở đầu danh sách để làm mới và ảnh sản phẩm mở chế độ xem phóng to/thu nhỏ, số lượng được ghim trên ảnh và thông tin giá/nhập hàng có thể thu gọn. Nút quay lại từ màn hình khác đưa về Kho và cuộn lên đầu; khi đã ở đầu danh sách, lần quay lại tiếp theo thoát ứng dụng. Menu ngăn kéo trượt tập trung tìm kiếm, thêm sản phẩm, mã QR nhận tiền, tổng quan, sản phẩm gần đây, bộ lọc tất cả/sắp hết/đã ẩn, báo cáo, quản lý kho và cài đặt; các mục tất cả và đã ẩn hiển thị số sản phẩm. Quản lý kho cho phép tạo/chuyển kho; nhấn giữ một kho để đổi tên, xóa hoặc xuất riêng sản phẩm ra ZIP. Mỗi kho dùng cơ sở dữ liệu riêng nên sản phẩm, đơn hàng, lịch sử, tìm kiếm, bộ lọc, cảnh báo và vị trí nút thêm không dùng chung; kho hiện tại trước đây được giữ nguyên làm kho mặc định khi cập nhật. Thanh trên có vùng chạm lớn cho menu, tên kho, tìm kiếm và thông báo; thanh dưới chỉ hiện biểu tượng, có kiểu tiêu chuẩn/bo tròn và được đẩy lên theo vùng điều hướng hệ thống để tránh bị che ở cả chế độ nút và cử chỉ. Kính mờ mặc định tắt; bật công tắc trong phần Giao diện để làm mờ nền phía sau thanh trên, thanh dưới và các nút nổi. Khi tắt, các thành phần này không khởi tạo BlurView. Có thể chỉnh chiều cao thanh tab và khoảng cách giữa các nút trong phần Giao diện. Thông báo sắp hết có thể đánh dấu đã đọc; sản phẩm mới chạm ngưỡng sắp hết sẽ tạo cảnh báo mới. Các nút nổi Thêm sản phẩm, QR và Lọc đều có thể di chuyển bằng giữ 1 giây rồi kéo; biểu tượng được thu nhỏ trong nút nền lớn. Các ô nhập số dùng bàn phím số và tự phân cách hàng nghìn. Cài đặt có bảng chọn màu kéo-thả, độ trong suốt và xác nhận xóa toàn bộ sản phẩm.

Màn hình **Mã QR nhận tiền** cho phép lưu nhiều tài khoản, mỗi tab có ảnh QR, tên người nhận, số tài khoản và ngân hàng. Mũi tên chuyển giữa các tab; nút quản lý mở lựa chọn thêm hoặc sửa, danh sách quản lý tab có menu sửa/xóa và hoàn tác xóa trong 3 giây. Ảnh mã QR được lưu cục bộ trên thiết bị; ảnh chọn từ thư viện hoặc chụp có thể cắt và xoay trước khi lưu.

Tab tìm kiếm hiển thị tên, số lượng tồn và giá bán theo từng loại; chạm ảnh để phóng to. Nút ba chấm mở bảng chi tiết gọn có giá bán, nút sửa và nút xem trong kho; xem trong kho tự cuộn đến sản phẩm và mở phần nhập hàng/sửa giá. Các hộp thoại dùng nút X góc trên bên phải để đóng. Bộ lọc tìm kiếm và kho cho phép chọn Cái/Kg/Tất cả, rồi sắp xếp tăng hoặc giảm theo tên, số lượng tồn hoặc giá bán; nút lọc có thể di chuyển bằng cách nhấn giữ 1 giây rồi kéo. “Sản phẩm gần đây” là tab riêng hiển thị tối đa 20 sản phẩm mới nhất. Màu phụ được dùng cho ảnh chưa có ảnh và các trạng thái lựa chọn; màu chữ tùy chỉnh áp dụng cho văn bản, biểu tượng và nút công tắc. Nhãn số loại có thể đổi bằng cách nhấn giữ 2 giây; xóa nhãn sẽ ẩn cả nhãn và số đếm nhưng giữ nguyên dữ liệu loại.

Cài đặt giao diện dùng biểu tượng cho chế độ theo hệ thống, sáng, tối và tùy chỉnh; các màu được chọn bằng nút màu gọn có thể kéo-thả và chỉnh độ trong. Màu chữ/biểu tượng mặc định tự tương phản trắng hoặc đen với nền; chỉ khi bật tùy chỉnh mới hiện điều khiển màu chữ riêng. Màu chủ đạo mặc định xanh trời, nền sáng xanh rất nhạt và nền tối xanh đen. Các tùy chọn tab và kính mờ được thu gọn/mở rộng. Có thể xuất/nhập ZIP gồm sản phẩm, đơn vị, tồn hiện tại, giá và ảnh. Khi nhập tên trùng, người dùng chọn bỏ qua, cập nhật tồn nhưng giữ giá hiện có, hoặc tạo sản phẩm mới với tên gợi ý có thể sửa. Đơn hàng cũ được giữ nguyên.

Chọn ảnh dùng thư viện ảnh riêng trong ứng dụng, hỗ trợ các định dạng ảnh mà Android giải mã được và quyền truy cập ảnh đầy đủ hoặc ảnh được chọn riêng trên Android 14 trở lên. Camera chụp ảnh độ phân giải đầy đủ; ảnh lớn hơn 5 MB được giảm còn tối đa 720p, trên 10 MB còn tối đa 480p, ảnh nhỏ hơn giữ nguyên độ phân giải. Ảnh từ thư viện, camera hoặc ứng dụng chia sẻ đều mở màn hình cắt/chỉnh ảnh (mặc định tỉ lệ vuông 1:1; có thể chọn tự do, dọc/ngang, xoay và lật) trước khi lưu; thanh công cụ chừa vùng điều hướng điện thoại. Bộ cắt dùng Android Image Cropper của CanHub (Apache-2.0). Có thể chia sẻ ảnh từ thư viện hoặc ứng dụng khác vào Quản lý kho để tạo sản phẩm mới hoặc cập nhật ảnh cho sản phẩm hiện có. Thông báo thêm sản phẩm hiện thành banner nhỏ phía trên, có thanh tiến trình và tự đóng sau 2 giây, không khóa thao tác trên màn hình.

Đăng nhập Google dùng phiên Firebase được lưu trên thiết bị; tài khoản đã đăng nhập trước đó vẫn mở được ứng dụng khi mất mạng và dữ liệu kho tiếp tục đọc/ghi trong Room. Nút tài khoản ở đầu menu mở bảng tài khoản; có thể thêm/đổi tài khoản qua Credential Manager hoặc đăng xuất. Cài đặt cho phép đăng nhập/đăng xuất và đặt ảnh cá nhân riêng cho từng tài khoản. Khi đăng nhập tài khoản chưa có kho cục bộ, ứng dụng hỏi dùng kho hiện có trên thiết bị hay tạo kho mới. Khi chưa có phiên Google, ứng dụng vào kho ngoại tuyến thay vì tự mở màn hình đăng nhập; chạm tài khoản để đăng nhập khi cần.

Web, duyệt nhập của nhân viên, trả hàng, chỉnh sửa đơn đã hoàn tất, báo cáo nâng cao và thu hồi thành viên riêng lẻ chưa có.
