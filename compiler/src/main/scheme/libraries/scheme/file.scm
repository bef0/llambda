(define-library (scheme file)
  (import (llambda nfi))
  (import (rename (llambda internal primitives) (define-report-procedure define-r7rs)))
  (import (only (scheme base) call-with-port current-input-port current-output-port))

  (include-library-declarations "../../interfaces/scheme/file.scm")

  (begin
    (define-native-library llfile (static-library "ll_scheme_file"))

    (define-r7rs file-exists? (native-function llfile "llfile_file_exists" (-> <string> <native-bool>)))

    (define-r7rs open-input-file (world-function llfile "llfile_open_input_file" (-> <string> <port>)))
    (define-r7rs open-binary-input-file open-input-file)

    (define-r7rs (call-with-input-file [path : <string>] [proc : (-> <port> <any>)])
                 (call-with-port (open-input-file path) proc))

    (define-r7rs (with-input-from-file [path : <string>] [proc : (-> <any>)])
                 (call-with-input-file path (lambda ([port : <port>])
                                              (parameterize ((current-input-port port))
                                                            (proc)))))

    (define-r7rs open-output-file (world-function llfile "llfile_open_output_file" (-> <string> <port>)))
    (define-r7rs open-binary-output-file open-output-file)

    (define-r7rs (call-with-output-file [path : <string>] [proc : (-> <port> <any>)])
                 (call-with-port (open-output-file path) proc))

    (define-r7rs (with-output-to-file [path : <string>] [proc : (-> <any>)])
                 (call-with-output-file path (lambda ([port : <port>])
                                               (parameterize ((current-output-port port))
                                                             (proc)))))

    (define-r7rs delete-file (world-function llfile "llfile_delete_file" (-> <string> <unit>)))))
