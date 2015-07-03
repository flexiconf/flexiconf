package se.blea.flexiconf

import java.io.File
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import scala.collection.mutable.ListBuffer


/** Helpers for visitors */
object FileUtil {
  def resolvePathsForGlob(basePath: String, pattern: String): List[String] = {
    val resolvedPattern = if (pattern.startsWith("/")) {
      pattern
    } else {
      basePath + "/" + pattern
    }

    val matcher = FileSystems.getDefault.getPathMatcher(s"glob:$resolvedPattern")
    val matches = ListBuffer[String]()

    Files.walkFileTree(Paths.get(basePath), new SimpleFileVisitor[Path]() {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        if (matcher.matches(file)) {
          matches.append(file.toString)
        }

        FileVisitResult.CONTINUE
      }
    })

    matches.toList
  }

  /** Resolve a file path for includes based on the location of the current file **/
  def resolvePath(basePath: String, path: String): Path = {
    Paths.get(basePath).resolveSibling(path).normalize
  }
}
