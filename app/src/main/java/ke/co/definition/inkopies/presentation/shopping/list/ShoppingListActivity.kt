package ke.co.definition.inkopies.presentation.shopping.list

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import com.google.gson.Gson
import ke.co.definition.inkopies.App
import ke.co.definition.inkopies.R
import ke.co.definition.inkopies.databinding.ActivityShoppingListBinding
import ke.co.definition.inkopies.databinding.ContentShoppingListBinding
import ke.co.definition.inkopies.databinding.ItemShoppingListBinding
import ke.co.definition.inkopies.model.shopping.ShoppingMode
import ke.co.definition.inkopies.presentation.common.InkopiesActivity
import ke.co.definition.inkopies.presentation.shopping.common.VMShoppingList
import ke.co.definition.inkopies.presentation.shopping.common.VMShoppingListItem

class ShoppingListActivity : InkopiesActivity() {

    private lateinit var viewModel: ShoppingListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val views: ActivityShoppingListBinding = DataBindingUtil.setContentView(this,
                R.layout.activity_shopping_list)

        val vmFactory = (application as App).appComponent.provideShoppingListVMFactory()
        viewModel = ViewModelProviders.of(this, vmFactory)
                .get(ShoppingListViewModel::class.java)
        views.vm = viewModel

        val viewAdapter = prepRecyclerView(views.content)

        observeViewModel(viewModel, views, viewAdapter)
        start(viewModel)
        observeViews(views, viewModel, viewAdapter)

        setSupportActionBar(views.toolbar)
        supportActionBar!!.title = viewModel.shoppingList.get()!!.name()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        viewModel.onCreateOptionsMenu()
        viewModel.menuRes.observe(this, Observer {
            menu?.clear()
            menuInflater.inflate(it!!, menu)
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.showProfile -> showProfile()
            R.id.logout -> logout()
            R.id.checkout -> viewModel.onCheckout()
            R.id.modePreparation -> viewModel.onChangeMode(ShoppingMode.PREPARATION)
            R.id.modeShopping -> viewModel.onChangeMode(ShoppingMode.SHOPPING)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun prepRecyclerView(vs: ContentShoppingListBinding): ShoppingListAdapter {

        val viewManager = LinearLayoutManager(this)
        val viewAdapter = ShoppingListAdapter()

        vs.shoppingLists.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        return viewAdapter
    }

    private fun observeViews(vs: ActivityShoppingListBinding, vm: ShoppingListViewModel, va: ShoppingListAdapter) {
        vs.fab.setOnClickListener {
            UpsertListItemDialogFrag.start(supportFragmentManager, vm.shoppingList.get()!!, null, null,
                    { vm.onItemAdded(it ?: return@start) })
        }
        va.setOnItemSelectedListener(object : ActionListener {
            override fun onItemSelected(item: VMShoppingListItem, pos: Int, focus: ItemFocus) {
                UpsertListItemDialogFrag.start(supportFragmentManager, vm.shoppingList.get()!!, item, focus,
                        {
                            if (it != null) {
                                vm.onItemUpdated(item, it, pos)
                            } else {
                                vm.onItemDeleted(item, pos)
                            }
                        })
            }

            override fun onCheckChanged(item: VMShoppingListItem, pos: Int, newState: Boolean) {
                vm.onItemSelectionChanged(item, pos, newState)
            }
        })
    }

    private fun observeViewModel(vm: ShoppingListViewModel, vs: ActivityShoppingListBinding, va: ShoppingListAdapter) {
        vm.snackbarData.observe(this, Observer { it?.show(vs.root) })
        vm.nextPage.observe(this, Observer { va.addItems(it ?: return@Observer) })
        vm.itemUpdate.observe(this, Observer {
            va.updateItem(it?.first ?: return@Observer, it.second)
        })
        vm.newItem.observe(this, Observer { va.add(it ?: return@Observer) })
        vm.itemDelete.observe(this, Observer { va.removeItem(it ?: return@Observer) })
        vm.clearList.observe(this, Observer { if (it == true) va.clear() })

        observedLiveData.addAll(mutableListOf(vm.snackbarData, vm.nextPage, vm.itemUpdate,
                vm.newItem, vm.itemDelete, vm.clearList))
    }

    private fun start(vm: ShoppingListViewModel) {
        val slStr = intent.getStringExtra(EXTRA_SHOPPING_LIST)
        val sl = Gson().fromJson(slStr, VMShoppingList::class.java)
        vm.start(sl)
    }

    companion object {

        private const val EXTRA_SHOPPING_LIST = "EXTRA_SHOPPING_LIST"

        fun start(activity: Activity, shoppingList: VMShoppingList) {
            val i = Intent(activity, ShoppingListActivity::class.java)
            i.putExtra(EXTRA_SHOPPING_LIST, Gson().toJson(shoppingList))
            activity.startActivity(i)
        }
    }

    interface ActionListener {
        fun onItemSelected(item: VMShoppingListItem, pos: Int, focus: ItemFocus)
        fun onCheckChanged(item: VMShoppingListItem, pos: Int, newState: Boolean)
    }

    class ShoppingListAdapter :
            RecyclerView.Adapter<ShoppingListAdapter.ItemShoppingListHolder>() {

        private var listener: ActionListener = object : ActionListener {
            override fun onItemSelected(item: VMShoppingListItem, pos: Int, focus: ItemFocus) {
                /* No-op */
            }

            override fun onCheckChanged(item: VMShoppingListItem, pos: Int, newState: Boolean) {
                /* No-op */
            }
        }

        private var items: MutableList<VMShoppingListItem> = mutableListOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemShoppingListHolder {

            val infl = LayoutInflater.from(parent.context)
            val views: ItemShoppingListBinding = DataBindingUtil.inflate(infl,
                    R.layout.item_shopping_list, parent, false)
            return ItemShoppingListHolder(views)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: ItemShoppingListHolder, bindPos: Int) {

            val item = items[bindPos]
            holder.binding.item = item

            holder.binding.brand.setOnClickListener {
                listener.onItemSelected(item, items.indexOf(item), ItemFocus.BRAND)
            }
            holder.binding.name.setOnClickListener {
                listener.onItemSelected(item, items.indexOf(item), ItemFocus.ITEM)
            }
            holder.binding.root.setOnClickListener {
                listener.onItemSelected(item, items.indexOf(item), ItemFocus.ITEM)
            }
            holder.binding.measuringUnit.setOnClickListener {
                listener.onItemSelected(item, items.indexOf(item), ItemFocus.MEASUREMENT_UNIT)
            }
            holder.binding.unitPrice.setOnClickListener {
                listener.onItemSelected(item, items.indexOf(item), ItemFocus.UNIT_PRICE)
            }
            holder.binding.x.setOnClickListener {
                listener.onItemSelected(item, items.indexOf(item), ItemFocus.QUANTITY)
            }
            holder.binding.quantity.setOnClickListener {
                listener.onItemSelected(item, items.indexOf(item), ItemFocus.QUANTITY)
            }
            holder.binding.checked.setOnCheckedChangeListener { _, state ->
                if (item.isChecked() == state) return@setOnCheckedChangeListener
                listener.onCheckChanged(item, items.indexOf(item), state)
            }
        }

        fun setOnItemSelectedListener(l: ActionListener) {
            listener = l
        }

        fun clear() {
            synchronized(this) {
                items.clear()
                notifyDataSetChanged()
            }
        }

        fun addItems(items: MutableList<VMShoppingListItem>) {
            synchronized(this) {
                val origiSize = this.items.size
                this.items.addAll(items)
                notifyItemRangeInserted(origiSize, items.size)
            }
        }

        fun updateItem(newVal: VMShoppingListItem, pos: Int) {
            synchronized(this) {
                this.items.removeAt(pos)
                val newPos = calculateNewPos(newVal)
                this.items.add(newPos, newVal)
                if (newPos == pos) {
                    notifyItemChanged(pos)
                } else {
                    notifyItemMoved(pos, newPos)
                }
            }
        }

        fun removeItem(pos: Int) {
            synchronized(this) {
                this.items.removeAt(pos)
                notifyItemRemoved(pos)
            }
        }

        fun add(item: VMShoppingListItem) {
            synchronized(this) {
                val newPos = calculateNewPos(item)
                items.add(newPos, item)
                notifyItemInserted(newPos)
            }
        }

        private fun calculateNewPos(item: VMShoppingListItem): Int {
            synchronized(this) {

                if (items.size == 0) {
                    return 0
                }

                var firstUncheckedPos = items.indexOfFirst { !it.isChecked() }
                if (firstUncheckedPos == -1) {
                    firstUncheckedPos = items.size
                }

                return if (item.isChecked()) {
                    calculateNewPos(item, 0, firstUncheckedPos)
                } else {
                    calculateNewPos(item, firstUncheckedPos, items.size)
                }
            }
        }

        /**
         * calculateNewPos calculates the position of item in the items list limited to the
         * range defined by offsetPos (inclusive) and lastPos (exclusive). Returns 0 if the list
         * is empty or lastPos is 0.
         */
        private fun calculateNewPos(item: VMShoppingListItem, offsetPos: Int, lastPos: Int): Int {
            synchronized(this) {

                if (items.size == 0 || lastPos == 0) {
                    return 0
                }

                val orderPos = items
                        .subList(offsetPos, lastPos)
                        .indexOfFirst { it.itemName() >= item.itemName() }
                return if (orderPos == -1) {
                    lastPos
                } else {
                    offsetPos + orderPos
                }
            }
        }

        data class ItemShoppingListHolder(internal val binding: ItemShoppingListBinding) :
                RecyclerView.ViewHolder(binding.root)

    }

}
