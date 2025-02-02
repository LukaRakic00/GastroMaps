package com.example.gastromaps.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.gastromaps.R

abstract class SwipeToCallback(context: Context) : ItemTouchHelper.SimpleCallback(
    0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
) {

    private val deleteBackground = ColorDrawable(Color.RED)
    private val editBackground = ColorDrawable(ContextCompat.getColor(context, R.color.colorPrimary))
    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete_white)
    private val editIcon = ContextCompat.getDrawable(context, R.drawable.ic_edit_white)
    private val intrinsicWidth = deleteIcon?.intrinsicWidth ?: 0
    private val intrinsicHeight = deleteIcon?.intrinsicHeight ?: 0
    private val paint = Paint().apply { color = Color.WHITE }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top

        // Draw background
        when {
            dX > 0 -> { // Swiping to the right (edit)
                editBackground.setBounds(
                    itemView.left,
                    itemView.top,
                    itemView.left + dX.toInt(),
                    itemView.bottom
                )
                editBackground.draw(c)

                // Calculate position for edit icon
                val editIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                val editIconMargin = (itemHeight - intrinsicHeight) / 2
                val editIconLeft = itemView.left + editIconMargin
                val editIconRight = itemView.left + editIconMargin + intrinsicWidth
                val editIconBottom = editIconTop + intrinsicHeight

                editIcon?.setBounds(editIconLeft, editIconTop, editIconRight, editIconBottom)
                editIcon?.draw(c)
            }
            dX < 0 -> { // Swiping to the left (delete)
                deleteBackground.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                deleteBackground.draw(c)

                // Calculate position for delete icon
                val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                val deleteIconMargin = (itemHeight - intrinsicHeight) / 2
                val deleteIconLeft = itemView.right - deleteIconMargin - intrinsicWidth
                val deleteIconRight = itemView.right - deleteIconMargin
                val deleteIconBottom = deleteIconTop + intrinsicHeight

                deleteIcon?.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
                deleteIcon?.draw(c)
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}