package edu.minervia.platform.service.email

enum class EmailTemplate(val templateName: String) {
    VERIFICATION("verification"),
    WELCOME("welcome"),
    REJECTION("rejection"),
    ALERT("alert")
}
