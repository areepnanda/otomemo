package com.otomemo.app

class WorkshopsFragment : MasterListFragment(R.layout.fragment_list) {

    private lateinit var workshops: MutableList<Workshop>

    override fun loadRows(): List<MasterRow> {
        workshops = Db.loadWorkshops(requireContext())
        return workshops.map { MasterRow(it.id, it.name, "") }
    }

    override fun onAdd() = textDialog("Tambah Bengkel", null) {
        val workshop = Workshop(System.currentTimeMillis(), it)
        workshops.add(workshop)
        Db.saveWorkshops(requireContext(), workshops)
        SheetSync.upsert(requireContext(), "workshops", workshop.toJson())
    }

    override fun onEdit(row: MasterRow) {
        val workshop = workshops.first { it.id == row.id }
        textDialog("Edit Bengkel", workshop.name) {
            workshop.name = it
            Db.saveWorkshops(requireContext(), workshops)
            SheetSync.upsert(requireContext(), "workshops", workshop.toJson())
        }
    }

    override fun onDelete(row: MasterRow) {
        workshops.removeAll { it.id == row.id }
        Db.saveWorkshops(requireContext(), workshops)
        SheetSync.delete(requireContext(), "workshops", row.id)
    }
}
