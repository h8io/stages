# Диаграмма простого pipeline

Для начала я бы хотел продемонстрировать работу pipeline на базе 3 stages, каждая из которых имеет 2 шага эволюции.
Так как цель этого примера только демонстрация, я использую только самые базовые определения Stage из модуля `core`.
Первая цифра в названии Stage — порядковый номер Stage, вторая — номер поколения pipeline.

Этот пример не является реальным использованием и предназначен только для демонстрации поведения stages.

Сначала я определю stages. Все методы, которые не должны быть вызваны, определены как `???`.
Для уменьшения шума я сначала определю базовый класс `Evolution`, в котором все методы определены как `???`,
а в примерах буду переопределять только те, которые нужны.

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

`Stage 1-1` вычитает из входного значения 3, в случае статуса Error всей pipeline эволюционирует в Stage 1-2.

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

`Stage 1-2` вычитает из 5 входное значение.

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

`Stage 2-1` проверяет, является ли нулем результат. Если результат не 0, то возвращает превращенный в строку результат.

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

`Stage 2-2` просто превращает входное значение в строку.

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

`Stage 3-1` вызывает только метод `skip()`, возвращающий в случае статуса `Error` `Stage 3-2`.

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

`Stage 3-2` проверяет наличие символа `'-'` в строке.

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

Создаем pipeline:

```scala mdoc
val pipeline1 = Stage11 ~> Stage21 ~> Stage31
```

Как видите, тип pipeline - `Stage[Int, Boolean, String]`. Иными словами, любая композиция stages с помощью
оператора `~>` имеет тип `Stage[?, ?, ?]`.

Теперь попробуем запустить pipeline:

```scala mdoc
val yld = pipeline1(3)
```

Обратите внимание, эволюция происходит в обратном порядке применениям Stage.

Тип Status - `Status.Error`:

```scala mdoc
yld.status.getClass
```

Для того чтобы получить следующее поколение pipeline, достаточно вызвать метод `evolve()` у `Yield`:

```scala mdoc
val pipeline2 = yld.evolve()
```

Тип остается тем же, что и у исходной `pipeline1`. Запустим второе поколение:

```scala mdoc
val yld2 = pipeline2(1)
```

По завершению вызываем метод `dispose()` у `Evolution`:

```scala mdoc
yld2.evolution.dispose()
```

Обратите внимание, `dispose()` вызывается в обратном к применениям Stage порядке, как и эволюция.

## Диаграмма

![Pipeline](Diagram.svg)

На диаграмме номерами в кругах помечены условные номера шагов выполнения. Пунктирные линии обозначают
последовательность использования объектов.

1. `in 1` (`== 3`) отправляется на вход `Stage 1-1`.
2. `Stage 1-1` создает `Yield.Some 1-1` со значением `out` (`== 0`). Также `Yield.Some 1-1` содержит
   `status` (`== Success`) и `evolution`, но они предназначены только для агрегации, и будут использованы
   только по окончании этого шага эволюции ("generation") pipeline.
3. `out` отправляется на вход `Stage 2-1`.
4. `Stage 2-1` создает `Yield.None 2-1` (`status == Error("Zero")`).
5. Так как `Stage 2-1` возвращает `Yield.None`, то выполнение pipeline останавливается, а в `Stage 3-1` вызывается
   метод `skip()`, создающий объект эволюции.
6. Из `Yield.Some 1-1` и `Yield.None 2-1` извлекаются объекты `Status` и собираются в общий статус pipeline
   (статус первого поколения - `Error("Zero")`).
7. На основании статуса pipeline первого поколения вызывается соответствующий метод
   объекта эволюции (`onError`), созданного методом `skip()` в (5).
8. Объект эволюции создает `Stage 3-2`.
9. На основании статуса pipeline первого поколения вызывается соответствующий метод объекта эволюции
   из `Yield.None 2-1`, созданного в (4).
10. Объект эволюции создает `Stage 2-2`.
11. На основании статуса pipeline первого поколения вызывается соответствующий метод объекта эволюции 
    из `Yield.Some 1-1`, созданного в (2).
12. Объект эволюции создает `Stage 1-2`.
13. `in 2` (`== 1`) отправляется на вход `Stage 1-2`.
14. `Stage 1-2` создает `Yield.Some 1-2` со значением `out` (`== 4`). Также `Yield.Some 1-2` содержит
    `status` (`== Complete`) и `evolution`, но они предназначены только для агрегации, и будут использованы
    только по окончании этого шага эволюции pipeline.
15. `out` отправляется на вход `Stage 2-2`.
16. `Stage 2-2` создает `Yield.Some 2-2` (`out = "4"`, `status == Success`).
17. `out` отправляется на вход `Stage 3-2`.
18. `Stage 3-2` создает `Yield.Some 3-2` (`out = false`, `status == Success`).
19. Из `Yield.Some 1-2`, `Yield.Some 2-2` и `Yield.Some 3-2` извлекаются объекты `Status`
    и собираются в общий статус pipeline (статус второго поколения `Complete`). На этом основное выполнение 
    pipeline завершено.
20. В объекте эволюции из `Yield.Some 3-2` вызывается метод `dispose`.
21. В объекте эволюции из `Yield.Some 2-2` вызывается метод `dispose`.
22. В объекте эволюции из `Yield.Some 1-2` вызывается метод `dispose`.
23. Если какие-то вызовы `dispose()` завершились с ошибкой, то они агрегируются в одно исключение,
    где "корневым" является самое раннее.
24. Из значения `out` (`== false`), статуса pipeline второго поколения (`Complete`) и опциональной ошибки dispose
    создается объект `outcome`, являющийся итоговым результатом выполнения pipeline.
tri