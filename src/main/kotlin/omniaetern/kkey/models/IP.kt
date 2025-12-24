package omniaetern.kkey.models

data class IP(val address: String) {
    companion object {
        fun parse(input: String): IP? {
            val trimmed = input.trim().removeSurrounding("[", "]") // Handle [::1] -> ::1

            if (trimmed.isBlank()) return null

            val isIpv4 = trimmed.split(".").size == 4 && trimmed.all { it.isDigit() || it == '.' }

            val isIpv6 = trimmed.contains(":") && !trimmed.contains(" ")

            val isLocal = trimmed == "localhost"
            return if (isIpv4 || isIpv6 || isLocal) {
                IP(trimmed)
            } else {
                null
            }
        }
    }

    val urlSafeAddress: String
        get() = if (address.contains(":") && !address.startsWith("[")) "[$address]" else address
}
