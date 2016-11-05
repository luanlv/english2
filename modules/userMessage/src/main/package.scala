package lila

package object userMessage extends PackageObject with WithPlay {

  private[userMessage] type ID = String

  object tube {
    // expose user tube
//    implicit lazy val listMenuTube = ListMenu.tube inColl Env.current.listMenuColl
//    implicit lazy val listViewTube = ListView.tube inColl Env.current.listMenuColl
  }

}
