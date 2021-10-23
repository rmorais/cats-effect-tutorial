package example

import cats.effect.IO
import java.io.File
import cats.effect.Resource
import java.io.FileInputStream
import java.io.FileOutputStream

object Hello extends App {

  def inputStream(f: File): Resource[IO, FileInputStream] = {
    Resource.make {
      IO.blocking(new FileInputStream(f))
    } { is => IO.blocking(is.close()).handleErrorWith(_ => IO.unit) }
  }

  def outputStream(f: File): Resource[IO, FileOutputStream] =
    Resource.make { IO.blocking(new FileOutputStream(f)) } { os => IO.blocking(os.close()).handleErrorWith(_ => IO.unit) }

  def transmit(is: FileInputStream, os: FileOutputStream, buffer: Array[Byte], acc: Long): IO[Long] = {
    for {
      amount <- IO.blocking(is.read(buffer, 0, buffer.size))
      count <- if (amount > -1) IO.blocking(os.write(buffer, 0, amount)) >> transmit(is, os, buffer, acc + amount) else IO.pure(acc)
    } yield count
  }
  def transfer(is: FileInputStream, os: FileOutputStream): IO[Long] = 
    transmit(is, os, new Array[Byte](1024 * 10), 0L)

  def copy(origin: File, destination: File): IO[Long] =
    (for {
      is <- inputStream(origin)
      os <- outputStream(destination)
    } yield (is, os)).use { case (is, os) => transfer(is, os) }
}
