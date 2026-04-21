# Диаграмма простого pipeline

В этом примере я покажу не «реальный» pipeline, а его упрощенную модель.  
Цель здесь не в том, чтобы продемонстрировать все возможности библиотеки, а в том, чтобы на одном небольшом сценарии увидеть три ключевые идеи:

- как stages применяются друг за другом;
- как pipeline сворачивает статусы отдельных stages в общий статус;
- почему методы Evolution вызываются в порядке, обратном порядку применения stages, и что это гарантирует.

Pipeline будет состоять из трех stages. У каждой из них есть две версии, то есть два поколения.  
В имени stage первая цифра обозначает ее номер в pipeline, а вторая — поколение pipeline, в котором используется эта версия stage.

Поскольку пример демонстрационный, я использую только базовые определения `Stage` из модуля `core`.  
Все методы, которые в этом сценарии вызываться не должны, определены как `???`.

Чтобы не загромождать пример однотипными заглушками, сначала введем вспомогательный базовый класс `MockEvolution`.  
В нем все методы уже определены как `???`, поэтому в конкретных stages можно переопределять только те из них, которые действительно участвуют в примере.

```scala mdoc
import h8io.stages.*

trait MockEvolution[-I, +O, +E] extends Evolution[I, O, E] {
  override def onSuccess(): Stage[I, O, E] = ???
  override def onComplete(): Stage[I, O, E] = ???
  override def onError(): Stage[I, O, E] = ???
  
  override def dispose(): Unit = ???
}
```

## Stage 1

Первая stage работает с целыми числами.

`Stage 1-1` получает входное значение, вычитает из него `3` и возвращает результат со статусом `Success`.  
Если по итогам всего запуска pipeline нужно эволюционировать по ветке `Error`, она перейдет в `Stage 1-2`.

```scala mdoc
object Stage11 extends Stage[Int, Int, Nothing] {
  override def apply(in: Int): Yield[Int, Int, Nothing] = {
    println(s"Apply Stage 1-1 to $in (${in.getClass.getSimpleName})")
    Yield.Some(
      in - 3,
      Status.Success,
      new MockEvolution[Int, Int, Nothing] {
        override def onError(): Stage[Int, Int, Nothing] = {
          println("Evolve Stage 1-1")
          Stage12
        }
      })
  }

  override def skip(): Evolution[Int, Int, Nothing] = ???
}
```

`Stage 1-2` — это следующая версия первой stage. Она вычисляет `5 - in` и возвращает статус `Complete`.  
В этом примере ее дальнейшая эволюция уже не важна, зато важно, что при завершении она участвует в `dispose()`.

```scala mdoc
object Stage12 extends Stage[Int, Int, Nothing] {
  override def apply(in: Int): Yield[Int, Int, Nothing] = {
    println(s"Apply Stage 1-2 to $in (${in.getClass.getSimpleName})")
    Yield.Some(
      5 - in,
      Status.Complete,
      new MockEvolution[Int, Int, Nothing] {
        override def dispose(): Unit = println("Dispose Stage 1-2")
      })
  }

  override def skip(): Evolution[Int, Int, Nothing] = ???
}
```

## Stage 2

Вторая stage преобразует число в строку, но в первом поколении делает дополнительную проверку.

`Stage 2-1` смотрит, равен ли вход нулю.  
Если вход равен `0`, она возвращает `Yield.None` со статусом `Error("Zero")`. Это означает, что дальше по pipeline значение уже не пойдет.  
Если же вход не равен нулю, stage просто превращает число в строку и возвращает `Success`.

В случае ошибки `Stage 2-1` эволюционирует в `Stage 2-2`.

```scala mdoc
object Stage21 extends Stage[Int, String, String] {
  override def apply(in: Int): Yield[Int, String, String] = {
    println(s"Apply Stage 2-1 to $in (${in.getClass.getSimpleName})")
    if (in == 0)
      Yield.None(
        Status.Error("Zero"),
        new MockEvolution[Int, String, String] {
          override def onError(): Stage[Int, String, String] = {
            println("Evolve Stage 2-1")
            Stage22
          }
        })
    else
      Yield.Some(in.toString, Status.Success, new MockEvolution[Int, String, String] {})
  }

  override def skip(): Evolution[Int, String, String] = ???
}
```

`Stage 2-2` — более простая версия второй stage: она всегда преобразует входное значение в строку.  
Как и `Stage 1-2`, в этом примере она интересна прежде всего как часть второго поколения pipeline и как участник финального `dispose()`.

```scala mdoc
object Stage22 extends Stage[Int, String, String] {
  override def apply(in: Int): Yield[Int, String, String] = {
    println(s"Apply Stage 2-2 to $in (${in.getClass.getSimpleName})")
    Yield.Some(
      in.toString,
      Status.Success,
      new MockEvolution[Int, String, String] {
        override def dispose(): Unit = println("Dispose Stage 2-2")
      })
  }

  override def skip(): Evolution[Int, String, String] = ???
}
```

## Stage 3

Третья stage работает уже со строками и в первом поколении вообще не применяется напрямую.

`Stage 3-1` в этом сценарии существует только для того, чтобы показать роль `skip()`.  
Если предыдущая stage не выдает значения и выполнение pipeline останавливается раньше времени, downstream stage все равно должна получить шанс корректно эволюционировать. Именно для этого и вызывается `skip()`.

`skip()` возвращает `Evolution` — тот же тип, что содержит обычный `Yield`. Pipeline вызывает у него тот же метод (`onError`, `onComplete` или `onSuccess`), что и у прочих stages.

В нашем примере `Stage 3-1` по ветке `Error` эволюционирует в `Stage 3-2`.

```scala mdoc
object Stage31 extends Stage[String, Boolean, Nothing] {
  override def apply(in: String): Yield[String, Boolean, Nothing] = ???

  override def skip(): Evolution[String, Boolean, Nothing] = {
    println("Skip Stage 3-1")
    new MockEvolution[String, Boolean, Nothing] {
      override def onError(): Stage[String, Boolean, Nothing] = {
        println("Evolve Stage 3-1")
        Stage32
      }
    }
  }
}
```

`Stage 3-2` — это уже рабочая версия третьей stage. Она проверяет, содержит ли входная строка символ дефиса, и возвращает результат типа `Boolean`.

```scala mdoc
object Stage32 extends Stage[String, Boolean, Nothing] {
  override def apply(in: String): Yield[String, Boolean, Nothing] = {
    println(s"Apply Stage 3-2 to $in (${in.getClass.getSimpleName})")
    Yield.Some(
      in.contains("-"),
      Status.Success,
      new MockEvolution[String, Boolean, Nothing] {
        override def dispose(): Unit = println("Dispose Stage 3-2")
      })
  }

  override def skip(): Evolution[String, Boolean, Nothing] = ???
}
```

## Выполнение

Теперь соберем pipeline из первых поколений всех трех stages:

```scala mdoc
val pipeline1 = Stage11 ~> Stage21 ~> Stage31
```

Тип получившегося pipeline — `Stage[Int, Boolean, String]`.  
Иными словами, композиция через `~>` сама тоже остается `Stage`: она принимает вход первого звена, выдает выход последнего и сворачивает статусы отдельных stages в общий статус pipeline.

Запустим первое поколение pipeline:

```scala mdoc
val yld = pipeline1(3)
```

Итоговый статус первого поколения — `Status.Error`:

```scala mdoc
yld.status.getClass
```

Когда запуск завершен, из полученного `Yield` можно построить следующее поколение pipeline:

```scala mdoc
val pipeline2 = yld.evolve()
```

Тип pipeline при этом не меняется. Меняется только то, какие именно версии stages теперь входят в композицию.

Запустим второе поколение:

```scala mdoc
val yld2 = pipeline2(1)
```

После завершения второго поколения вызовем `dispose()` у его итоговой `Evolution`:

```scala mdoc
yld2.evolution.dispose()
```

## Диаграмма

![Pipeline](Diagram.svg)

Диаграмма показывает тот же сценарий, что и код, но уже как последовательность конкретных объектов и вызовов.  
Метки в кружках обозначают шаги выполнения, поэтому в описании ниже я буду ссылаться на них напрямую.

В первом поколении входное значение `3` поступает в `Stage 1-1` ①.  
`Stage 1-1` вычисляет `3 - 3`, возвращает `Yield.Some` со значением `0`, статусом `Success` и объектом `Evolution` ②.  
Полученное значение передается дальше, в `Stage 2-1` ③.

В `Stage 2-1` pipeline приходит к развилке, от которой зависит дальнейшее выполнение.  
Поскольку вход равен `0`, эта stage возвращает `Yield.None` со статусом `Error("Zero")` ④.  
После этого передавать значение дальше уже нечего, поэтому `Stage 3-1` не вызывается через `apply()`. Вместо этого для нее вызывается `skip()`, который создает объект `Evolution` ⑤.

Когда активная часть первого поколения завершена, pipeline сворачивает уже полученные `Status` в общий статус ⑥.  
В данном случае итоговым статусом первого поколения становится `Error("Zero")`.

Теперь, когда этот статус известен, pipeline может эволюционировать в следующее поколение.  
Все методы `Evolution` вызываются в порядке, обратном порядку применения stages. Это важно потому, что downstream stage может зависеть от состояния или ресурсов, созданных upstream stage. Если завершать или перестраивать upstream раньше времени, downstream stage может потерять то, на что еще опирается.

Сначала используется объект, полученный через `skip()` у `Stage 3-1`: по ветке `onError()` он создает `Stage 3-2` ⑦ ⑧.  
Затем по той же причине эволюционирует `Stage 2-1`: ее `Evolution` переводит pipeline к `Stage 2-2` ⑨ ⑩.  
После этого эволюционирует `Stage 1-1`, превращаясь в `Stage 1-2` ⑪ ⑫.

Так формируется второе поколение pipeline.

Во втором поколении на вход pipeline подается значение `1` ⑬.  
`Stage 1-2` вычисляет `5 - 1`, возвращает `Yield.Some` со значением `4`, статусом `Complete` и новым объектом `Evolution` ⑭.  
Затем значение `4` передается в `Stage 2-2` ⑮, которая преобразует его в строку `"4"` и возвращает `Yield.Some` со статусом `Success` ⑯.  
После этого строка `"4"` поступает в `Stage 3-2` ⑰, а та проверяет, содержит ли она символ `'-'`, и возвращает `Yield.Some` со значением `false` и статусом `Success` ⑱.

Когда все три stage второго поколения отработали, pipeline сворачивает их статусы в общий статус ⑲.  
Итоговым статусом второго поколения становится `Complete`. На этом вычислительная часть pipeline заканчивается.

Далее начинается завершение.  
Сначала вызывается `dispose()` у `Evolution`, связанной с результатом `Stage 3-2` ⑳.  
Затем `dispose()` вызывается у `Evolution`, связанной с результатом `Stage 2-2` ㉑.  
Наконец, `dispose()` вызывается у `Evolution`, связанной с результатом `Stage 1-2` ㉒.

Если какие-либо вызовы `dispose()` завершаются с ошибкой, эти ошибки агрегируются в одно исключение ㉓.  
Самое раннее исключение становится основным, а все последующие присоединяются к нему.

После этого из трех компонентов собирается итоговый `Outcome` ㉔:

- выходного значения второго поколения (`false`);
- итогового статуса pipeline (`Complete`);
- и, при необходимости, ошибки, возникшей во время `dispose()`.

Именно этот `Outcome` и представляет собой окончательный результат выполнения pipeline.
