"""Boundary cases independent of the real case's expected outcome."""
import unittest
from screen_domain_isoforms import amino_blocks, deletion_positions, translate_complete
from validate_domain_isoforms import indistinguishable, occurrences


class IsoformBoundaryTests(unittest.TestCase):
    def test_complete_cds_rejects_ambiguous_internal_stop_and_partial(self):
        self.assertEqual('MA', translate_complete('ATGGCCTAA'))
        for dna in ['ATGGCCT', 'ATGGCC', 'ATGNNNTAA', 'ATGTAGGCCTAA', 'GTGGCCTAA']:
            self.assertIsNone(translate_complete(dna), dna)

    def test_deletion_ambiguity_is_not_resolved_arbitrarily(self):
        self.assertEqual([2], deletion_positions('ABXYZCD', 'ABCD', 2))
        self.assertEqual([1, 2, 3], deletion_positions('MAAAK', 'MAAK', 1))
        self.assertEqual([], deletion_positions('ABXYZCD', 'ABCE', 2))

    def test_split_codon_on_both_strands(self):
        plus = {'strand': '+', 'cds': [[101, 104, 0], [201, 208, 2]]}
        minus = {'strand': '-', 'cds': [[201, 204, 0], [101, 108, 2]]}
        self.assertEqual([[104, 104], [201, 202]], amino_blocks(plus, 2, 2))
        self.assertEqual([[201, 201], [107, 108]], amino_blocks(minus, 2, 2))
        with self.assertRaises(ValueError):
            amino_blocks(plus, 4, 5)

    def test_isoleucine_leucine_and_overlapping_matches(self):
        self.assertEqual(indistinguishable('AILK'), indistinguishable('ALLK'))
        self.assertEqual([0, 1, 2], list(occurrences('AAA', 'AAAAA')))


if __name__ == '__main__':
    unittest.main()
