package ke.co.definition.inkopies.views

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ke.co.definition.inkopies.R
import ke.co.definition.inkopies.databinding.FragmentShoppingListPlanBinding
import ke.co.definition.inkopies.model.Model
import ke.co.definition.inkopies.model.beans.ShoppingList
import ke.co.definition.inkopies.model.beans.ShoppingListBrand

/**
 * Created by tomogoma on 09/07/17.
 */
class ShoppingListPlanFragment : Fragment() {

    companion object {

        private val CLASS_NAME = ShoppingListPlanFragment::class.java.name!!
        private val EXTRA_SHOPPING_LIST = CLASS_NAME + "EXTRA_SHOPPING_LIST"

        fun initialize(sl: ShoppingList): ShoppingListPlanFragment {
            val f = ShoppingListPlanFragment()
            val args = Bundle()
            args.putSerializable(EXTRA_SHOPPING_LIST, sl)
            f.arguments = args
            return f
        }
    }

    interface PriceSettable {
        fun onTotalPricesChange(totals: Pair<Float, Float>)
        fun onNewShoppingListBrandComplete(successful: Boolean)
    }

    private lateinit var adapter: ShoppingListBrandAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentShoppingListPlanBinding>(
                inflater, R.layout.fragment_shopping_list_plan, container, false)
        val sl = arguments.getSerializable(EXTRA_SHOPPING_LIST) as ShoppingList
        binding.items.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
        binding.items.layoutManager = layoutManager
        adapter = ShoppingListBrandAdapter(sl, context)
        val did = DividerItemDecoration(context, layoutManager.orientation)
        binding.items.addItemDecoration(did)
        binding.items.adapter = adapter

        adapter.onPriceChange = { newPrices ->
            (activity as PriceSettable).onTotalPricesChange(newPrices)
        }

        adapter.onNewShoppingListBrandComplete = { successful ->
            (activity as PriceSettable).onNewShoppingListBrandComplete(successful)
        }

        loadList(sl)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (activity !is PriceSettable) {
            throw RuntimeException("Activity needs to implement PriceSettable interface")
        }
    }

    fun newShoppingListBrand() {
        val pos = layoutManager.findFirstVisibleItemPosition()
        adapter.newShoppingListBrand(if (pos < 0) 0 else pos)
    }

    fun loadList(sl: ShoppingList) {
        Model.getShoppingListBrands(sl.currMode!!, sl.id!!, resultCallback = { res ->
            val r: MutableList<ShoppingListBrand> = res as MutableList
            if (r.isEmpty()) {
                adapter.newShoppingListBrand(0)
                return@getShoppingListBrands
            }
            adapter.setShoppingListBrands(r)
        })
    }
}
