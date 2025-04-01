package cn.treedeep.link.core.util;

public class DatetimeUtil {

    /**
     * 格式化时间间隔，根据时长自动选择合适的单位
     *
     * @param millis 毫秒数
     * @return 格式化后的时间字符串
     */
    public static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + "毫秒";
        } else if (millis < 60000) {
            return String.format("%.2f秒", millis / 1000.0);
        } else if (millis < 3600000) {
            long minutes = millis / 60000;
            long seconds = (millis % 60000) / 1000;
            return String.format("%d分%d秒", minutes, seconds);
        } else {
            long hours = millis / 3600000;
            long minutes = (millis % 3600000) / 60000;
            long seconds = (millis % 60000) / 1000;
            return String.format("%d小时%d分%d秒", hours, minutes, seconds);
        }
    }
}
