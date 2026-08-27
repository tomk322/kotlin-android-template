package com.tomk322.rootscope.ui

import com.tomk322.rootscope.model.Report

/** UI state for the single screen. */
sealed interface ScanState {
    data object Idle : ScanState
    data class Running(val message: String) : ScanState
    data class Done(val report: Report) : ScanState
}
