package lila

package object activity extends PackageObject with WithPlay {

  private[activity] type ID = String

  object tube {
    // expose user tube
    //    implicit lazy val listMenuTube = ListMenu.tube inColl Env.current.listMenuColl
    //    implicit lazy val listViewTube = ListView.tube inColl Env.current.listMenuColl
  }

}
