circuit AndGate :
  module AndGate :
    input in0 : UInt<1>
    input in1 : UInt<1>
    output out : UInt<1>

    out <= and(in0, in1)
