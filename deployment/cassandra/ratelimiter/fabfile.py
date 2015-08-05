from fabric.api import hosts, task
from fabric.contrib.project import rsync_project


@hosts('ubuntu@0.prod.ratelimiter.bstore.bloomreach.com')
@task
def deploy(path='/mnt/nodestaging'):
  """
  Deploy rate limiter frontend.
  """

  extra_opts = "--omit-dir-times --recursive"
  local_dirs = ["."]
  for local_dir in local_dirs:
    rsync_project(
        remote_dir=path,
        local_dir=local_dir,
        delete=True,
        extra_opts=extra_opts,
    )
