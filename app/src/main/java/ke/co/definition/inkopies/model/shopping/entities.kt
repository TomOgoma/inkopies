package ke.co.definition.inkopies.model.shopping

import ke.co.definition.inkopies.model.stores.StoreBranch

/**
 * Created by tomogoma
 * On 25/03/18.
 */

data class Category(
        val id: String,
        val name: String
)

data class MeasuringUnit(
        val id: String,
        val name: String
)

data class ShoppingItem(
        val id: String,
        val name: String
)

data class Brand(
        val id: String,
        val name: String,
        val measuringUnit: MeasuringUnit,
        val shoppingItem: ShoppingItem
) {
    fun measuringUnitName() = measuringUnit.name
    fun itemName() = shoppingItem.name
}

data class BrandPrice(
        val id: String,
        val price: Float,
        val brand: Brand,
        val atStoreBranch: StoreBranch
) {
    fun measuringUnit() = brand.measuringUnit
    fun measuringUnitName() = brand.measuringUnitName()
    fun item() = brand.shoppingItem
    fun itemName() = brand.itemName()
    fun brandName() = brand.name
    fun storeBranchName() = atStoreBranch.name
    fun storeName() = atStoreBranch.storeName()
    fun store() = atStoreBranch.store
    fun storeBranchID() = atStoreBranch.id
    fun brandID() = brand.id
}

data class ShoppingListItem(
        val id: String,
        val quantity: Int,
        val brandPrice: BrandPrice,
        val category: Category,
        val inList: Boolean,
        val inCart: Boolean
) {
    fun measuringUnit() = brandPrice.measuringUnit()
    fun measuringUnitName() = brandPrice.measuringUnitName()
    fun item() = brandPrice.item()
    fun itemName() = brandPrice.itemName()
    fun brand() = brandPrice.brand
    fun brandName() = brandPrice.brandName()
    fun unitPrice() = brandPrice.price
    fun totalPrice() = unitPrice() * quantity
    fun categoryName() = category.name
    fun storeBranchName() = brandPrice.storeBranchName()
    fun storeName() = brandPrice.storeName()
    fun store() = brandPrice.store()
    fun storeBranchID() = brandPrice.storeBranchID()
    fun brandID() = brandPrice.brandID()
}

data class ShoppingListItemInsert(
        val shoppingListID: String,
        val itemName: String,
        val inList: Boolean,
        val inCart: Boolean,
        val categoryName: String? = null,
        val brandName: String? = null,
        val quantity: Int? = null,
        val measuringUnit: String? = null,
        val unitPrice: Float? = null
)

data class ShoppingListItemUpdate(
        val shoppingListID: String,
        val shoppingListItemID: String,
        val itemName: String? = null,
        val inList: Boolean? = null,
        val inCart: Boolean? = null,
        val categoryName: String?,
        val brandName: String? = null,
        val quantity: Int? = null,
        val measuringUnit: String? = null,
        val unitPrice: Float? = null,
        val storeBranchName: String? = null,
        val storeName: String? = null
)

data class ShoppingListItemsFilter(
        val shoppingListID: String,
        val inList: Boolean? = null,
        val inCart: Boolean? = null
)

data class ShoppingListItemSearch(
        val brandName: String?,
        val shoppingItemName: String?,
        val brandPrice: String?,
        val measUnit: String?
)

enum class ShoppingMode {
    PREPARATION, SHOPPING
}

data class ShoppingList(
        val id: String,
        val name: String,
        val activeListPrice: Float,
        val cartPrice: Float,
        val mode: ShoppingMode = ShoppingMode.PREPARATION
) {

    fun validate(): Throwable? {
        if (name == "") {
            return Exception("name cannot be empty")
        }
        return null
    }

    fun accumulateInsertPrices(totalPrice: Float, inList: Boolean, inCart: Boolean): ShoppingList {

        var newListPrice = activeListPrice
        var newCartPrice = cartPrice
        if (inList) {
            newListPrice += totalPrice
        }
        if (inCart) {
            newCartPrice += totalPrice
        }
        return ShoppingList(id, name, newListPrice, newCartPrice, mode)
    }

    fun accumulateDeletePrices(toDelete: ShoppingListItem)
            : ShoppingList {

        var newListPrice = activeListPrice
        var newCartPrice = cartPrice
        if (toDelete.inList) {
            newListPrice -= toDelete.totalPrice()
        }
        if (toDelete.inCart) {
            newCartPrice -= toDelete.totalPrice()
        }
        return ShoppingList(id, name, newListPrice, newCartPrice, mode)
    }

    fun accumulateUpdatePrices(curr: ShoppingListItem, update: ShoppingListItemUpdate): ShoppingList {

        var newListPrice = activeListPrice
        var newCartPrice = cartPrice

        var newTotalPrice = curr.totalPrice()
        // Have change in total price.
        if (update.unitPrice != null || update.quantity != null) {

            newTotalPrice =
                    (update.unitPrice ?: curr.unitPrice()) * (update.quantity ?: curr.quantity)
            val priceDelta = newTotalPrice - curr.totalPrice()

            if (curr.inList) {
                newListPrice += priceDelta
            }

            if (curr.inCart) {
                newCartPrice += priceDelta
            }
        }

        if (update.inList != null) {
            when {
            // Added to list.
                !curr.inList && update.inList -> newListPrice += newTotalPrice
            // Removed from list.
                curr.inList && !update.inList -> newListPrice -= newTotalPrice
            }
        }

        if (update.inCart != null) {
            when {
            // Added to cart.
                !curr.inCart && update.inCart -> newCartPrice += newTotalPrice
            // Removed from cart.
                curr.inCart && !update.inCart -> newCartPrice -= newTotalPrice
            }
        }

        return ShoppingList(id, name, newListPrice, newCartPrice, mode)
    }
}