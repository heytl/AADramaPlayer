package com.aa.duanju.utils

object EpisodeParser {

    /**
     * 正则表达式匹配常见的集数命名规则
     * 常见格式：
     * - 第1集, 第01集, 第一集
     * - EP01, Ep 1, ep1, episode1
     * - 01.mp4, 1.mp4
     * - 剧名-1.mp4
     */
    private val REGEX_PATTERNS = listOf(
        Regex("第\\s*(\\d+)\\s*(集|部分|话)"),
        Regex("(?i)ep\\s*(\\d+)"),
        Regex("(?i)episode\\s*(\\d+)"),
        Regex("[-_\\s](\\d+)(?=\\.[a-zA-Z0-9]+$)"), // 紧挨着扩展名数字：_01.mp4
        Regex("^(\\d+)(?=\\.[a-zA-Z0-9]+$)")      // 纯数字开头：1.mp4
    )

    /**
     * 尝试从文件名中提取集数
     * @param fileName 不带路径的纯文件名
     * @return 解析到的集数（默认返回 1，若真无法识别则放在前面）
     */
    fun parseEpisodeNumber(fileName: String): Int {
        for (pattern in REGEX_PATTERNS) {
            val matchResult = pattern.find(fileName)
            if (matchResult != null) {
                // group(1) 往往是捕获的数字部分
                val strNum = matchResult.groups[1]?.value
                if (!strNum.isNullOrEmpty()) {
                    try {
                        return strNum.toInt()
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        
        // 兜底策略：如果上面正则都没匹配到，提取名字里最后一个数字集合
        val fallbackRegex = Regex("(\\d+)")
        val fallbackMatches = fallbackRegex.findAll(fileName)
        if (fallbackMatches.any()) {
            return fallbackMatches.last().value.toIntOrNull() ?: 1
        }
        
        return 1
    }

    /**
     * 判断是否是视频文件
     */
    fun isVideoFile(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return lower.endsWith(".mp4") ||
               lower.endsWith(".mkv") ||
               lower.endsWith(".avi") ||
               lower.endsWith(".flv") ||
               lower.endsWith(".mov")
    }
}
