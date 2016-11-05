package lila

package object question extends PackageObject with WithPlay {

  private[question] type ID = String

  object tube {
    // expose user tube
    //    implicit lazy val listMenuTube = ListMenu.tube inColl Env.current.listMenuColl
    //    implicit lazy val listViewTube = ListView.tube inColl Env.current.listMenuColl
  }

}
