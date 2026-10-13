package com.otomemo.app

class ItemsFragment : MasterListFragment(R.layout.fragment_list) {

    private lateinit var items: MutableList<ServiceItem>

    override fun loadRows(): List<MasterRow> {
        items = Db.loadItems(requireContext())
        return items.map { MasterRow(it.id, it.name, "") }
    }

    override fun onAdd() = textDialog("Tambah Item Servis", null) {
        items.add(ServiceItem(System.currentTimeMillis(), it))
        Db.saveItems(requireContext(), items)
    }

    override fun onEdit(row: MasterRow) {
        val item = items.first { it.id == row.id }
        textDialog("Edit Item Servis", item.name) {
            item.name = it
            Db.saveItems(requireContext(), items)
        }
    }

    override fun onDelete(row: MasterRow) {
        items.removeAll { it.id == row.id }
        Db.saveItems(requireContext(), items)
    }
}
