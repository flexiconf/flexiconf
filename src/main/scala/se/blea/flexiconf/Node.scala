package se.blea.flexiconf

/**
 * Created by tblease on 2/17/15.
 */
private[flexiconf] case class Node[T](value: T,
                                   source: Source,
                                   children: List[Node[T]]) {

}
