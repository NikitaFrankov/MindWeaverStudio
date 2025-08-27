package com.example.mindweaverstudio.data.utils.sourcecode.models

sealed class SearchResult {
    data class Success(val matches: List<SourceCodeMatch>) : SearchResult()
    data class NotFound(val targetName: String, val searchedFiles: Int) : SearchResult()
    data class Error(val message: String, val cause: Throwable? = null) : SearchResult()
    data class MultipleMatches(val matches: List<SourceCodeMatch>) : SearchResult()
}

fun SearchResult.getFirstMatch(): SourceCodeMatch? = when (this) {
    is SearchResult.Success -> matches.firstOrNull()
    is SearchResult.MultipleMatches -> matches.firstOrNull()
    else -> null
}