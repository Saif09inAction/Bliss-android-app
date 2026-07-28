package com.laiza.worker.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.laiza.worker.R
import com.laiza.worker.domain.models.OrderStatus

@Composable
fun OrderStatus.kaarigerDisplayName(): String = when (this) {
    OrderStatus.ASSIGNED -> stringResource(R.string.kaariger_status_in_progress)
    OrderStatus.PENDING_APPROVAL -> stringResource(R.string.kaariger_status_pending_approval)
    OrderStatus.COMPLETED -> stringResource(R.string.kaariger_status_completed)
    OrderStatus.REJECTED -> stringResource(R.string.kaariger_status_rejected)
}
