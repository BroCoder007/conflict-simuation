import unittest
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'backend/src/main/resources/data'
def read(name):return json.loads((DATA/name).read_text(encoding='utf-8'))

class DataContracts(unittest.TestCase):
    def test_source_country_mapping_and_unique_keys(self):
        countries=read('countries.json');ids={c['id'] for c in countries}
        self.assertEqual(len(ids),len(countries))
        history=read('history.json');self.assertEqual(len(history),len({(r['country'],r['year']) for r in history}))
        for row in history:self.assertIn(row['country'],ids)
        for row in read('candidate.json'):self.assertIn(row['country'],ids)
    def test_fatality_ranges_and_category_totals(self):
        flagged={(q['country'],q['period']) for q in read('manifest.json')['qualityIssues']}
        for row in read('history.json')+read('candidate.json'):
            self.assertGreaterEqual(row['low'],0)
            if not row['low']<=row['best']<=row['high']:
                self.assertIn((row['country'],str(row.get('year',row.get('month')))),flagged)
            self.assertEqual(row['best'],row['stateBased']+row['nonState']+row['oneSided'])
    def test_conflict_period_and_count(self):
        rows=read('candidate.json');manifest=read('manifest.json')
        self.assertTrue(all('2026-01'<=r['month']<='2026-09' for r in rows));self.assertEqual(sum(r['events'] for r in rows),manifest['candidateEvents'])
    def test_predecessors_not_silently_assigned_to_current_countries(self):
        history=read('history.json')
        self.assertTrue(any(r['country']=='SUN' for r in history));self.assertFalse(any(r['country']=='RUS' and r['year']<1992 for r in history))
        self.assertFalse(any(r['country']=='SRB' and r['year']<2006 for r in history))
    def test_geography_is_bundled_and_has_no_event_coordinates(self):
        world=json.loads((ROOT/'frontend/public/data/world.geojson').read_text(encoding='utf-8'));landforms=json.loads((ROOT/'frontend/public/data/landforms.geojson').read_text(encoding='utf-8'))
        self.assertGreater(len(world['features']),150);self.assertGreater(len(landforms['features']),10)
        for row in read('candidate.json'):self.assertNotIn('latitude',row);self.assertNotIn('longitude',row)
    def test_metrics_preserve_years_and_missing_values(self):
        metrics=read('metrics.json')['countries'];self.assertIn('IND',metrics)
        for country in metrics.values():
            for metric in country.values():self.assertGreaterEqual(metric['year'],1960);self.assertGreaterEqual(metric['value'],0)

if __name__=='__main__':unittest.main()
