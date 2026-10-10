package com.example.finance;

import org.springframework.stereotype.Component;

/** Display wording only. Never changes stored status, audit records, or verification decisions. */
@Component("recipientMatchPresentation")
public class RecipientMatchPresentation {
    public static final String MATCH_EN = "Matched against simulated School Registry";
    public static final String MATCH_VI = "Khớp Danh bạ Nhà trường mô phỏng";
    public static final String SOURCE = "Simulated School Registry";

    public String text(String value) { return display(value); }

    public static String display(String value) {
        if (value == null) return null;
        String result = value.replace("Connected ", "Simulated ")
                .replace("account is not connected", "account is not linked in the simulation")
                                .replace("Account disconnected", "Simulated source unavailable")
                .replace("no connected Bank B account", "no linked Bank B source in the simulation")
                .replace("chưa kết nối tài khoản Bank B", "chưa liên kết nguồn Bank B trong mô phỏng");
        String[][] phrases = {
            {"Provider, beneficiary, receiving bank, account, corridor and currency match the trusted demo registry", "Provider, beneficiary, receiving bank, account, corridor and currency match the Simulated School Registry"},
            {"One or more beneficiary fields do not match the trusted education-provider registry", "One or more beneficiary fields do not match the Simulated School Registry"},
            {"The beneficiary profile does not match the trusted education-provider registry.", "The beneficiary profile does not match the Simulated School Registry."},
            {"Verified Education Provider Registry", SOURCE},
            {"Education Provider Registry verification", "Simulated School Registry match"},
            {"Verified tuition-channel comparison", "Tuition-channel comparison · " + MATCH_EN},
            {"So sánh kênh học phí đã xác minh:", "So sánh kênh học phí · " + MATCH_VI + ":"},
            {"Choose the verified tuition bill", "Choose the tuition bill matched against simulated School Registry"},
            {"active, verified, unpaid tuition bill", "active, unpaid tuition bill matched against simulated School Registry"},
            {"active verified tuition bill", "active tuition bill matched against simulated School Registry"},
            {"active, unpaid, verified tuition bill", "active, unpaid tuition bill matched against simulated School Registry"},
            {"Verified tuition bill", "Tuition bill matched against simulated School Registry"},
            {"Verified bill", "Bill matched against simulated School Registry"},
            {"the verified bill", "the bill matched against simulated School Registry"},
            {"recorded, verified bills", "recorded bills matched against simulated School Registry"},
            {"a verified beneficiary", "a beneficiary matched against simulated School Registry"},
            {"verified active student bill", "active student bill matched against simulated School Registry"},
            {"Hóa đơn học phí đã xác minh ", "Hóa đơn học phí khớp Danh bạ Nhà trường mô phỏng "},
            {"Hóa đơn đã xác minh ", "Hóa đơn khớp Danh bạ Nhà trường mô phỏng "},
            {"hóa đơn học phí đang hoạt động, đã xác minh và chưa thanh toán", "hóa đơn học phí đang hoạt động, chưa thanh toán và khớp Danh bạ Nhà trường mô phỏng"},
            {"hóa đơn học phí đang hoạt động, chưa thanh toán và đã xác minh", "hóa đơn học phí đang hoạt động, chưa thanh toán và khớp Danh bạ Nhà trường mô phỏng"},
            {"hóa đơn và thông số đã ghi nhận, đã xác minh", "hóa đơn khớp Danh bạ Nhà trường mô phỏng và thông số đã ghi nhận"},
            {"hóa đơn học phí đã xác minh cần hỏi", "hóa đơn học phí khớp Danh bạ Nhà trường mô phỏng cần hỏi"},
            {"Recipient is unverified or mismatches the registry. Correct and verify the bill.", "Recipient has not matched or mismatches the Simulated School Registry. Correct and check the bill against that registry."},
            {"Người nhận chưa được xác minh hoặc không khớp registry. Hãy sửa và xác minh hóa đơn.", "Người nhận chưa khớp hoặc không khớp Danh bạ Nhà trường mô phỏng. Hãy sửa và đối chiếu hóa đơn với danh bạ này."}
        };
        for (String[] phrase : phrases) result = result.replace(phrase[0], phrase[1]);
        return result.replaceAll("(?m)^(Bill .+?) is verified", "$1 matches the simulated School Registry")
                .replaceAll("(?m)^(Tuition bill .+?) is verified", "$1 matches the simulated School Registry");
    }
}
