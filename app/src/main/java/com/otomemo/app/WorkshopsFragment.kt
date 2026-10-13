package com.otomemo.app

class WorkshopsFragment : MasterListFragment(R.layout.fragment_list) {

    private lateinit var workshops: MutableList<Workshop>

    override fun loadRows(): List<MasterRow> {
        workshops = Db.loadWorkshops(requireContext())
        return workshops.map { MasterRow(it.id, it.name, "") }
    }

    override fun onAdd() = textDialog("Tambah Bengkel", null) {
        workshops.add(Workshop(System.currentTimeMillis(), it))
        Db.saveWorkshops(requireContext(), workshops)
    }

    override fun onEdit(row: MasterRow) {
        val workshop = workshops.first { it.id == row.id }
        textDialog("Edit Bengkel", workshop.name) {
            workshop.name = it
            Db.saveWorkshops(requireContext(), workshops)
        }
    }

    override fun onDelete(row: MasterRow) {
        workshops.removeAll { it.id == row.id }
        Db.saveWorkshops(requireContext(), workshops)
    }
}
