const dbname = 'site-db';
const version = 1;
class Cache {
    open_database(name, version, reject) {
        // create a database
        let openRequest = indexedDB.open(name, version);

        openRequest.onerror = function (event) {
            reject(
                new Error(
                    // @ts-ignore
                    'Open Database error: ' + event.target.error.message,
                ),
            );
        };

        // if db does not exist(version should be taken into consideration)
        openRequest.onupgradeneeded = function (event) {
            // @ts-ignore
            let db = event.target.result;

            // get all tables
            let names = db.objectStoreNames;
            if (names == null || names.length === 0) {
                // if not tables, create what you want

                //  id, time, peer_id, msg, type(receive, send)
                // inline-key, with generator, see:
                // https://stackoverflow.com/questions/36016862/indexeddb-dataerror-data-provided-to-an-operation-does-not-meet-requirements
                db.createObjectStore('messages', {
                    keyPath: 'id',
                    autoIncrement: true,
                });
            } else {
                // if tables are not empty, do other things, like re-create these tables
                if (names.contains('messages')) {
                    db.deleteObjectStore('messages');
                    db.createObjectStore('messages', {
                        keyPath: 'id',
                        autoIncrement: true,
                    });
                }
            }
        };

        return openRequest;
    }

    /**
     * find records in database by predicate
     * @param predicate
     * @returns if success, the promise brings back the list of records(full fields)
     */
    filterRecords(
        predicate: (cursor: IDBCursorWithValue) => boolean,
    ): Promise<any[]> {
        return new Promise((resolve, reject) => {
            let openRequest = this.open_database(dbname, version, reject);
            openRequest.onsuccess = function (event) {
                // @ts-ignore
                let db = event.target.result;
                let transaction = db.transaction(['messages'], 'readonly');
                let objectStore = transaction.objectStore('messages');
                const getRequest = objectStore.openCursor();

                getRequest.onerror = function (event) {
                    reject(
                        new Error(
                            'Error filtering data from the database: ' +
                                // @ts-ignore
                                event.target.error.message,
                        ),
                    );
                };

                let results: any[] = [];
                getRequest.onsuccess = function (event) {
                    const cursor = event.target.result;
                    if (cursor) {
                        // Assuming 'cursor.value' is the object and 'field' is the field you want to search
                        if (predicate(cursor)) {
                            // cursor is the pair
                            results.push(cursor.value);
                        }
                        cursor.continue();
                    } else {
                        resolve(results);
                    }
                };
            };
        });
    }

    /**
     * get a record by key
     * @param key
     * @returns the promise brings back the record(full fields),if the record does not exist, return null;
     */
    getItem(key: number) {
        return new Promise((resolve, reject) => {
            let openRequest = this.open_database(dbname, version, reject);
            openRequest.onsuccess = function (event) {
                // @ts-ignore
                let db = event.target.result;
                let transaction = db.transaction(['messages'], 'readonly');
                let objectStore = transaction.objectStore('messages');
                let getRequest = objectStore.get(key);

                getRequest.onerror = function (event) {
                    reject(
                        new Error(
                            'Error reading data from the database: ' +
                                // @ts-ignore
                                event.target.error.message,
                        ),
                    );
                };

                getRequest.onsuccess = function (event) {
                    if (event.target.result) {
                        resolve(event.target.result);
                    } else {
                        resolve(null); // Resolve with null if no data is found
                    }
                };
            };
        });
    }

    /**
     * create or update a record; if the record with the key does not exist, create it; otherwise, update it
     * @param entity the record to be created or updated, may contain the key
     */
    setItem(entity: any) {
        return new Promise((resolve, reject) => {
            let openRequest = this.open_database(dbname, version, reject);
            openRequest.onsuccess = function (event) {
                // @ts-ignore
                let db = event.target.result;
                let transaction = db.transaction(['messages'], 'readwrite');
                let store = transaction.objectStore('messages');

                // inline-key, store directly
                let request = store.put(entity);

                request.onsuccess = function () {
                    resolve({
                        entity,
                    });
                    console.log('Data set to the database');
                };

                request.onerror = function (errEvn) {
                    reject(
                        new Error(
                            `Error setting data to the database: ${errEvn.target.error.message}`,
                        ),
                    );
                };
            };
        });
    }

    getAllKeys(): Promise<any[]> {
        return new Promise((resolve, reject) => {
            let openRequest = this.open_database(dbname, version, reject);
            openRequest.onsuccess = function (event) {
                // @ts-ignore
                let db = event.target.result;

                let transaction = db.transaction(['messages'], 'readonly');
                let store = transaction.objectStore('messages');
                let request = store.getAllKeys();

                request.onerror = function (event) {
                    reject(
                        new Error(
                            'Error retrieving keys from the database: ' +
                                event.target.error.message,
                        ),
                    );
                };

                request.onsuccess = function (event) {
                    resolve(event.target.result);
                };
            };
        });
    }

    removeItem(key) {
        return new Promise<void>((resolve, reject) => {
            let openRequest = this.open_database(dbname, version, reject);
            openRequest.onsuccess = function (event) {
                // @ts-ignore
                let db = event.target.result;

                let transaction = db.transaction(['messages'], 'readwrite');
                let store = transaction.objectStore('messages');
                let request = store.delete(key);

                request.onerror = function (event) {
                    reject(
                        new Error(
                            'Error deleting item from the database: ' +
                                event.target.error.message,
                        ),
                    );
                };

                request.onsuccess = function () {
                    resolve();
                };
            };
        });
    }
}

export default new Cache();
