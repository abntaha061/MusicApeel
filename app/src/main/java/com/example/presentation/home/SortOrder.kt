package com.example.presentation.home

enum class SortOrder(val displayName: String) {
    ALPHABETICAL_ASC("أبجديًا (من أ إلى ي)"),
    ALPHABETICAL_DESC("أبجديًا عكسيًا (من ي إلى أ)"),
    DATE_ADDED_DESC("التاريخ (من الأحدث إلى الأقدم)"),
    DATE_ADDED_ASC("التاريخ (من الأقدم إلى الأحدث)")
}
