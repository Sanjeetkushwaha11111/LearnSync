package com.example.reminder.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.example.reminder.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class QuizBottomSheetDialog : BottomSheetDialogFragment() {
    private var tvQuestion: TextView? = null
    private var rgOptions: RadioGroup? = null
    private var btnSubmit: Button? = null
    private var radioButtons: List<RadioButton>? = null
    private var currentQuestion: QuizQuestion? = null
    private var onAnswerSubmitted: ((Boolean) -> Unit)? = null
    private var pendingQuestion: QuizQuestion? = null
    private var pendingCallback: ((Boolean) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.quiz_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        setupSubmitButton()
        
        // Show pending question if exists
        pendingQuestion?.let { question ->
            pendingCallback?.let { callback ->
                showQuestion(question, callback)
                pendingQuestion = null
                pendingCallback = null
            }
        }
    }

    private fun setupViews(view: View) {
        tvQuestion = view.findViewById(R.id.tvQuestion)
        rgOptions = view.findViewById(R.id.rgOptions)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        radioButtons = listOf(
            view.findViewById(R.id.rbOption1),
            view.findViewById(R.id.rbOption2),
            view.findViewById(R.id.rbOption3),
            view.findViewById(R.id.rbOption4)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tvQuestion = null
        rgOptions = null
        btnSubmit = null
        radioButtons = null
    }

    private fun setupSubmitButton() {
        btnSubmit?.setOnClickListener {
            rgOptions?.let { group ->
                val selectedId = group.checkedRadioButtonId
                if (selectedId != -1) {
                    val selectedIndex = radioButtons?.indexOfFirst { it.id == selectedId } ?: -1
                    val isCorrect = currentQuestion?.let { question ->
                        question.correctAnswer == selectedIndex
                    } ?: false
                    onAnswerSubmitted?.invoke(isCorrect)
                    dismiss()
                }
            }
        }
    }

    fun showQuestion(question: QuizQuestion, onSubmit: (Boolean) -> Unit) {
        if (tvQuestion == null || rgOptions == null || radioButtons == null) {
            // Views not initialized yet, save for later
            pendingQuestion = question
            pendingCallback = onSubmit
            return
        }

        currentQuestion = question
        onAnswerSubmitted = onSubmit
        
        tvQuestion?.text = question.question
        rgOptions?.clearCheck()
        
        radioButtons?.let { buttons ->
            question.options.forEachIndexed { index, option ->
                buttons[index].text = option
            }
        }
    }

    companion object {
        fun newInstance(question: QuizQuestion, onSubmit: (Boolean) -> Unit): QuizBottomSheetDialog {
            return QuizBottomSheetDialog().apply {
                showQuestion(question, onSubmit)
            }
        }
    }
}
