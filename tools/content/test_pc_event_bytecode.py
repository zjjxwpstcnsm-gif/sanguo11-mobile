#!/usr/bin/env python3
"""Native bytecode accessors and malformed-boundary regression."""
import gzip
import json
import os
import struct
import unittest
from pathlib import Path
from inspect_pc_layered_scenario import NativeLayeredWorld
from inspect_pc_event_bytecode import BytecodePlatform, decode_program


class EventBytecodeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        installation = Path(os.environ['PC_INSTALLATION'])
        cls.world = NativeLayeredWorld((installation / 'san11pk.exe').read_bytes())
        cls.platform = BytecodePlatform(cls.world, installation)
        cls.world.call(0x73fd70)
        cls.pointer = cls.platform.allocate(0x10000)
        report = json.loads(gzip.decompress(Path(os.environ['PC_STARTED_REPORT']).read_bytes()))
        record = report['installedEventAssembly']['originalLazyBodyReferences'][0]
        cls.body = bytes.fromhex(record['bodyHex'])
        cls.condition = bytes.fromhex(record['metadataHex'])[64:]

    def test_actual_body_and_condition_native_instructions(self):
        before = bytes(self.world.u.mem_read(0x7200000, 0x300000))
        rng = bytes(self.world.u.mem_read(0x8a5d44, 4))
        body = decode_program(self.world, self.pointer, self.body)
        condition = decode_program(self.world, self.pointer, self.condition)
        self.assertEqual(349, body['instructionCount'])
        self.assertEqual(37, condition['instructionCount'])
        self.assertEqual((4, 91, 20), tuple(body['instructions'][1][key] for key in ('opcode', 'operand1', 'operand2')))
        for program in (body, condition):
            self.assertFalse(program['conditionOrEffectExecuted'])
            self.assertTrue(all(row['nativeOpcodeValid'] and not row['executed'] for row in program['instructions']))
            self.assertEqual('', program['strings'][0]['text'])
        self.assertEqual(before, bytes(self.world.u.mem_read(0x7200000, 0x300000)))
        self.assertEqual(rng, bytes(self.world.u.mem_read(0x8a5d44, 4)))

    def test_native_invalid_opcode_preserved_and_never_executed(self):
        altered = bytearray(self.body)
        struct.pack_into('<I', altered, 52, 27)
        result = decode_program(self.world, self.pointer, bytes(altered))
        self.assertEqual(27, result['instructions'][1]['opcode'])
        self.assertFalse(result['instructions'][1]['nativeOpcodeValid'])
        self.assertFalse(result['instructions'][1]['executed'])

    def test_pointer_bounds_and_unexamined_header_rejected(self):
        altered = bytearray(self.body)
        struct.pack_into('<I', altered, 12, len(altered) + 1)
        with self.assertRaisesRegex(ValueError, 'instruction range escaped'):
            decode_program(self.world, self.pointer, bytes(altered))
        altered = bytearray(self.body)
        struct.pack_into('<I', altered, 8, 1)
        with self.assertRaisesRegex(ValueError, 'header version/flags'):
            decode_program(self.world, self.pointer, bytes(altered))


if __name__ == '__main__':
    unittest.main()
