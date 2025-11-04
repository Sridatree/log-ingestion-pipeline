import functools
import time

def measure_time(func):
    """
    Decorator that measures and prints the execution time of a function.
    """
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        start_time = time.time()
        result = func(*args, **kwargs)
        elapsed = time.time() - start_time
        print(f"{func.__name__} took {elapsed:.4f} seconds to execute")
        return result
    return wrapper