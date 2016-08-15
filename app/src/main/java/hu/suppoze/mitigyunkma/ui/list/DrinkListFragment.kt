package hu.suppoze.mitigyunkma.ui.list

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import hu.suppoze.mitigyunkma.R
import hu.suppoze.mitigyunkma.`interface`.ToolbarCollapseController
import hu.suppoze.mitigyunkma.ui.base.BaseFragment
import hu.suppoze.mitigyunkma.ui.base.Navigator
import hu.suppoze.mitigyunkma.ui.calculate.CalculatePresenter
import hu.suppoze.mitigyunkma.entity.Drink
import hu.suppoze.mitigyunkma.event.MainPagerFragmentChangedEvent
import kotlinx.android.synthetic.main.fragment_drinklist.*
import io.realm.Realm
import io.realm.Sort
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.support.v4.dimen

class DrinkListFragment(val sortByField: String) : BaseFragment() {

    val realm: Realm

    lateinit var toolbarCollapseController : ToolbarCollapseController

    init {
        realm = Realm.getDefaultInstance()
    }

    constructor() : this(Drink::lastmod.name)

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try {
            toolbarCollapseController = context as ToolbarCollapseController
        } catch (ex: ClassCastException) {
            throw RuntimeException("${activity.localClassName} must implement controller")
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_drinklist, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun setupRecyclerView() {
        drinkRecyclerView.setHasFixedSize(false)
        drinkRecyclerView.layoutManager = LinearLayoutManager(context)
        drinkRecyclerView.itemAnimator = DefaultItemAnimator()
        drinkRecyclerView.setAnimationEndListener { setToolbarCollapse(Navigator.selectedPage) }

        drinkRecyclerView.addItemDecoration(DrinkListItemDecoration(
                dimen(R.dimen.fragment_padding),
                dimen(R.dimen.drinklist_spacing),
                dimen(R.dimen.drinklist_first_and_last_padding_vertical)))

        val realmResultDataSet = realm.where(Drink::class.java)
                .findAllSorted(
                        sortByField,
                        if (sortByField == Drink::lastmod.name) Sort.DESCENDING else Sort.ASCENDING)

        drinkRecyclerView.adapter = DrinkListAdapter(realmResultDataSet, { edit(it) }, { delete(it) })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMainPagerFragmentChanged(event: MainPagerFragmentChangedEvent) {
        setToolbarCollapse(event.position)
    }

    private fun setToolbarCollapse(position: Int? = null) {
        if (position != Navigator.Pages.CALCULATE.ordinal) {
            val layoutManager = drinkRecyclerView.layoutManager as LinearLayoutManager
            if (layoutManager.findLastCompletelyVisibleItemPosition() == layoutManager.itemCount - 1) {
                toolbarCollapseController.turnOffToolbarScrolling()
            } else {
                toolbarCollapseController.turnOnToolbarScrolling()
            }
        }
    }

    private fun edit(drink: Drink) {
        CalculatePresenter.instance.loadDrinkForEdit(drink)
        Navigator.navigate(Navigator.Pages.CALCULATE)
    }

    private fun delete(drink: Drink) {
        realm.executeTransaction {
            drink.deleteFromRealm()
        }
    }

    class DrinkListItemDecoration(
            val horizontalPadding: Int,
            val verticalSpacing: Int,
            val firstAndLastVerticalPadding: Int
    ) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
            if (parent == null || outRect == null || state == null || view == null)
                return

            outRect.left = horizontalPadding
            outRect.right = horizontalPadding
            outRect.bottom = verticalSpacing
            outRect.top = verticalSpacing

            if ((view.layoutParams as RecyclerView.LayoutParams).isItemRemoved && parent.getChildLayoutPosition(view) == 0) {
                outRect.top = firstAndLastVerticalPadding
            } else if (parent.getChildAdapterPosition(view) == 0) {
                outRect.top = firstAndLastVerticalPadding
            }

            if (parent.getChildLayoutPosition(view) == state.itemCount - 1) {
                outRect.bottom = firstAndLastVerticalPadding
            }
        }
    }
}