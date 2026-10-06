import os
import contextlib
import io
from pathlib import Path
import subprocess
import tempfile
import unittest
from publish_release import publish


class PublishReleaseTests(unittest.TestCase):
    def invoke(self, responses):
        calls, sleeps = [], []
        def run(args, **kwargs):
            calls.append(args)
            return subprocess.CompletedProcess(args, responses.pop(0), 'url', 'temporary error')
        with tempfile.TemporaryDirectory() as directory:
            original = Path.cwd()
            try:
                os.chdir(directory); Path('release').mkdir()
                for name in ('mod.jar', 'mod.jar.sha256'): Path('release', name).write_text('test')
                with contextlib.redirect_stdout(io.StringIO()):
                    publish('v0.7.0', '0.7.0', 'mod.jar', 'abc', run, sleeps.append)
            finally: os.chdir(original)
        return calls, sleeps

    def test_existing_release_is_never_overwritten(self):
        calls, sleeps = self.invoke([0])
        self.assertEqual(1, len(calls)); self.assertFalse(sleeps)

    def test_create_retries_and_preserves_build_commit_and_files(self):
        calls, sleeps = self.invoke([1,1,1,0])
        self.assertEqual([5], sleeps)
        self.assertEqual(['gh','release','create','v0.7.0','release/mod.jar','release/mod.jar.sha256'], calls[-1][:6])
        self.assertIn('abc', calls[-1])

    def test_creation_that_succeeded_before_a_connection_error_is_not_repeated(self):
        calls, sleeps = self.invoke([1,1,0])
        self.assertEqual(3, len(calls)); self.assertEqual([5], sleeps)

    def test_publication_failure_is_reported(self):
        with self.assertRaises(RuntimeError): self.invoke([1]*6)


if __name__ == '__main__': unittest.main()
