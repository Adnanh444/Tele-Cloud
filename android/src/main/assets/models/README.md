# Bundled models

## MobileFaceNet.tflite

* **Purpose** — turns a 112×112 face crop into a 192-number signature. Two signatures are compared
  with cosine similarity to decide whether two faces are the same person.
* **Input** — `float32[2, 112, 112, 3]`, RGB, pre-processed as `(pixel - 127.5) / 128`, two faces
  per run.
* **Output** — `float32[2, 192]`.
* **Size** — 5,233,396 bytes.
* **SHA-256** — `d8ba40c0127fb8ca9917e8fddc79bbbda063657bc92a496d34da0bc8a760443b`.
* **Source** — `https://github.com/syaringan357/Android-MobileFaceNet-MTCNN-FaceAntiSpoofing`
  (`app/src/main/assets/MobileFaceNet.tflite`), MIT licensed; the tensor contract above was read
  from the file itself with a FlatBuffers parser, not from documentation.
* **Licence of this copy** — MIT (see the upstream `LICENSE`). No changes were made to the file.

Everything runs locally: no image, crop or signature is uploaded, and the model needs no network
connection or Play Services.
