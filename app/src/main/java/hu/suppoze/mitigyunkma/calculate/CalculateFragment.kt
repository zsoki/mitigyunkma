package hu.suppoze.mitigyunkma.calculate

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import butterknife.ButterKnife
import butterknife.Bind
import butterknife.OnClick

import hu.suppoze.mitigyunkma.R
import hu.suppoze.mitigyunkma.base.BaseFragment
import hu.suppoze.mitigyunkma.extensions.onFinishedAnimation
import hu.suppoze.mitigyunkma.util.ResourceHelper
import kotlinx.android.synthetic.main.dialog_save.view.*
import org.jetbrains.anko.support.v4.alert

class CalculateFragment : BaseFragment() {

    @Bind(R.id.calculate_view_capacity) lateinit var capacityField: TextInputLayout
    @Bind(R.id.calculate_view_percentage) lateinit var percentField: TextInputLayout
    @Bind(R.id.calculate_view_price) lateinit var priceField: TextInputLayout
    @Bind(R.id.component_drink_index_textview) lateinit var drinkIndex: TextView
    @Bind(R.id.button_action) lateinit var actionButton: RelativeLayout
    @Bind(R.id.action_button_text) lateinit var actionButtonText: TextView
    @Bind(R.id.button_action_background) lateinit var actionButtonBackground: RelativeLayout
    @Bind(R.id.button_reset) lateinit var resetButton: RelativeLayout

    // TODO: Create custom numpad view
    @Bind(R.id.numpad_0) lateinit var num0: Button
    @Bind(R.id.numpad_1) lateinit var num1: Button
    @Bind(R.id.numpad_2) lateinit var num2: Button
    @Bind(R.id.numpad_3) lateinit var num3: Button
    @Bind(R.id.numpad_4) lateinit var num4: Button
    @Bind(R.id.numpad_5) lateinit var num5: Button
    @Bind(R.id.numpad_6) lateinit var num6: Button
    @Bind(R.id.numpad_7) lateinit var num7: Button
    @Bind(R.id.numpad_8) lateinit var num8: Button
    @Bind(R.id.numpad_9) lateinit var num9: Button
    @Bind(R.id.numpad_delete) lateinit var numDelete: Button
    @Bind(R.id.numpad_decimal) lateinit var numDecimal: Button

    var currentActionButtonState: ActionButtonState = ActionButtonState.DISABLED
    lateinit var colorToState: Map<ActionButtonState, Int>
    lateinit var actionButtonTextToState: Map<ActionButtonState, String>

    val presenter: CalculatePresenter = CalculatePresenter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.onCreate();
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater!!.inflate(R.layout.fragment_calculate, container, false)
        ButterKnife.bind(this, view)

        percentField.editText?.addTextChangedListener(presenter)
        capacityField.editText?.addTextChangedListener(presenter)
        priceField.editText?.addTextChangedListener(presenter)

        // Disable soft input from appearing
        percentField.editText?.setOnTouchListener { view, motionEvent -> true }
        capacityField.editText?.setOnTouchListener { view, motionEvent -> true }
        priceField.editText?.setOnTouchListener { view, motionEvent -> true }

        initializeNumpad()

        colorToState = hashMapOf(
                Pair(ActionButtonState.DISABLED, ContextCompat.getColor(context, R.color.action_button_disabled)),
                Pair(ActionButtonState.NEXT, ContextCompat.getColor(context, R.color.action_button_next)),
                Pair(ActionButtonState.SAVE, ContextCompat.getColor(context, R.color.action_button_save))
        )

        actionButtonTextToState = hashMapOf(
                Pair(ActionButtonState.DISABLED, ResourceHelper.getStringRes(R.string.next)),
                Pair(ActionButtonState.NEXT, ResourceHelper.getStringRes(R.string.next)),
                Pair(ActionButtonState.SAVE, ResourceHelper.getStringRes(R.string.save))
        )

        switchActionButtonState(ActionButtonState.NEXT)

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    private fun initializeNumpad() {

        // TODO: Create custom numpad view
        num0.setOnClickListener { writeInFocused("0") }
        num1.setOnClickListener { writeInFocused("1") }
        num2.setOnClickListener { writeInFocused("2") }
        num3.setOnClickListener { writeInFocused("3") }
        num4.setOnClickListener { writeInFocused("4") }
        num5.setOnClickListener { writeInFocused("5") }
        num6.setOnClickListener { writeInFocused("6") }
        num7.setOnClickListener { writeInFocused("7") }
        num8.setOnClickListener { writeInFocused("8") }
        num9.setOnClickListener { writeInFocused("9") }
        numDecimal.setOnClickListener { writeInFocused(".") }
        numDelete.setOnClickListener { deleteFromFocused() }
    }


    private fun writeInFocused(s: String) {
        determineFocused()?.text?.append(s)
    }

    private fun deleteFromFocused() {
        val focusedTextField = determineFocused()?.text

        if (focusedTextField.isNullOrEmpty()) return
        focusedTextField?.delete(focusedTextField.length - 1, focusedTextField.length)
    }

    private fun determineFocused(): EditText? {
        if (capacityField.editText!!.isFocused) return capacityField.editText
        else if (percentField.editText!!.isFocused) return percentField.editText
        else if (priceField.editText!!.isFocused) return priceField.editText
        throw RuntimeException("CalculateFragment: Determining focus failed.")
    }

    fun switchActionButtonState(newActionButtonState: ActionButtonState) {
        if (currentActionButtonState != newActionButtonState) {
            animateActionButton(currentActionButtonState, newActionButtonState)
            currentActionButtonState = newActionButtonState
        }
        actionButton.isClickable = (newActionButtonState != ActionButtonState.DISABLED)
    }

    private fun animateActionButton(oldState: ActionButtonState, newState: ActionButtonState) {
        val fromColor = colorToState[oldState]
        val toColor = colorToState[newState]
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)

        colorAnimation.duration = 200
        colorAnimation.addUpdateListener { actionButtonBackground.setBackgroundColor(it.animatedValue as Int) }
        colorAnimation.start()

        val textColor = ContextCompat.getColor(context, R.color.button_text)
        val textFadeColor = ContextCompat.getColor(context, R.color.button_text_fade)
        animateActionButtonText(newState, textColor, textFadeColor)
    }

    private fun animateActionButtonText(newState: ActionButtonState, fromTextColor: Int, toTextColor: Int, repeat: Boolean = true) {
        val textColorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), fromTextColor, toTextColor)

        textColorAnimation.duration = 100
        textColorAnimation.addUpdateListener { actionButtonText.setTextColor(it.animatedValue as Int) }
        textColorAnimation.onFinishedAnimation {
            if (repeat) {
                actionButtonText.text = actionButtonTextToState[newState]
                animateActionButtonText(newState, toTextColor, fromTextColor, false)
            }
        }
        textColorAnimation.start()
    }

    @OnClick(R.id.button_reset)
    fun onResetClicked() {
        drinkIndex.text = ""

        percentField.editText!!.text.clear()
        capacityField.editText!!.text.clear()
        priceField.editText!!.text.clear()

        percentField.requestFocus()
    }

    @OnClick(R.id.button_action)
    fun onActionButtonClicked() {
        when (currentActionButtonState) {
            ActionButtonState.NEXT -> selectNextEmptyField()?.requestFocus()
            ActionButtonState.SAVE -> showSaveDialog()
            else -> {
                throw RuntimeException()
            }
        }
    }

    private fun showSaveDialog() {
        alert {
            title(R.string.dialog_save_title)

            val dialogView = View.inflate(context, R.layout.dialog_save, null)
            customView(dialogView)
            showKeyboard(context, dialogView.dialog_save_name)

            positiveButton(R.string.save) {
                hideKeyboard(context, dialogView.dialog_save_name)
                presenter.saveDrink(dialogView.dialog_save_name.editText!!.text.toString())
            }
            negativeButton(R.string.cancel) { hideKeyboard(context, dialogView.dialog_save_name) }
        }.show()
    }

    private fun selectNextEmptyField(): TextInputLayout? {
        if (percentField.editText!!.text.isNullOrEmpty()) return percentField
        if (priceField.editText!!.text.isNullOrEmpty()) return priceField
        if (capacityField.editText!!.text.isNullOrEmpty()) return capacityField
        else return null;
    }

    enum class ActionButtonState {
        NEXT, DISABLED, SAVE
    }

}