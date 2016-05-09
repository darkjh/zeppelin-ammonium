# zeppelin-ammonium

Connect `ammonium` shell to `zeppelin` notebook for all its goodies (Scala, Spark, plotly etc). 

## Usage
  1. Download zeppelin binary. `zeppelin-ammonium` currently targets the version `0.5.6-incubating`
  2. Clone this repo and `sbt assembly`
  3. Copy the assembly jar under `{zeppelin-bin-path}/interpreters/ammonium/`
  4. Edit `{zeppelin-bin-path}/conf/zeppelin-site.xml`'s `zeppelin.interpreters` property by adding `me.juhanlol.ScalaInterpreter` at the end
  5. Start the zeppelin server by `{zeppelin.interpreters}/bin/zeppelin-daemon.sh start`
  6. Go to `localhost:8080`, click on `interpreter` section, and create a new interpreter. There'll be a `scala` in the list.
  7. Create a notebook and type in `%scala` at the beginning of each cell and start using
