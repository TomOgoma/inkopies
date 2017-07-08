package ke.co.definition.inkopies.views

import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup

import ke.co.definition.inkopies.R
import ke.co.definition.inkopies.databinding.FragmentShoppingListsItemBinding
import ke.co.definition.inkopies.model.beans.ShoppingList

/**
 * Created by tomogoma on 08/07/17.
 */

class ShoppingListsListAdapter(private var shoppingLists: List<ShoppingList>?) : RecyclerView.Adapter<ShoppingListsListAdapter.ViewHolder>() {

    class ViewHolder(var layout: FragmentShoppingListsItemBinding) : RecyclerView.ViewHolder(layout.root)

    fun setShoppingLists(shoppingLists: List<ShoppingList>) {
        this.shoppingLists = shoppingLists
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingListsListAdapter.ViewHolder {
        val binding = DataBindingUtil.inflate<FragmentShoppingListsItemBinding>(
                LayoutInflater.from(parent.context),
                R.layout.fragment_shopping_lists_item,
                parent, false
        )
        val vh = ViewHolder(binding)
        return vh
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.layout.shoppingList = shoppingLists!![position]
    }

    override fun getItemCount(): Int {
        return shoppingLists?.size  ?: 0
    }
}
